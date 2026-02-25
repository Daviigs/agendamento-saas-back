package lash_salao_kc.agendamento_back.scheduler;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.service.TenantDateTimeService;
import lash_salao_kc.agendamento_back.service.TenantService;
import lash_salao_kc.agendamento_back.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler responsável pelo envio automático de lembretes de agendamentos.
 *
 * IMPORTANTE — Lógica de timezone:
 * Os agendamentos são armazenados como LocalDate + LocalTime no timezone local do tenant
 * (ex: 14:00 em America/Sao_Paulo). Para calcular corretamente o momento de envio do
 * lembrete, é OBRIGATÓRIO obter o "agora" no timezone do tenant, e não via
 * LocalDateTime.now() (que usa o timezone da JVM/servidor, geralmente UTC).
 *
 * Fluxo correto:
 * 1. Obtém "agora" no timezone do tenant → nowTenant (LocalDateTime)
 * 2. targetTime = nowTenant + minutosAntecedencia
 * 3. Busca agendamentos cuja data/hora local esteja na janela [targetTime ± 1min]
 * 4. Antes de enviar, faz verificação de segurança comparando Instants (UTC)
 * 5. Marca reminderSent = true (idempotência)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long SCHEDULER_INTERVAL_MS = 60_000; // 1 minuto

    private final AppointmentsRepository appointmentsRepository;
    private final WhatsappService whatsappService;
    private final TenantService tenantService;
    private final TenantDateTimeService tenantDateTimeService;

    /**
     * Método agendado que executa a cada minuto.
     * Processa lembretes para todos os tenants ativos.
     */
    @Scheduled(fixedRate = SCHEDULER_INTERVAL_MS)
    @Transactional
    public void sendReminders() {
        Instant schedulerStart = Instant.now();
        log.info("🔔 Iniciando verificação de lembretes | scheduler_start_utc={}",
                schedulerStart);

        List<String> tenantKeys = tenantService.getAllActiveTenants();
        log.info("👥 Tenants ativos: {}", tenantKeys);

        int totalReminders = 0;

        for (String tenantKey : tenantKeys) {
            try {
                TenantEntity tenant = tenantService.getTenantByKey(tenantKey);
                totalReminders += processRemindersForTenant(tenant);
            } catch (Exception e) {
                log.error("❌ Erro ao processar lembretes do tenant '{}': {}", tenantKey, e.getMessage(), e);
            }
        }

        log.info("🎯 Total de lembretes enviados: {} | duração_ms={}",
                totalReminders, Duration.between(schedulerStart, Instant.now()).toMillis());
    }

    /**
     * Processa lembretes de agendamentos de um tenant específico.
     *
     * A lógica central é:
     * 1. Obter "agora" no timezone do tenant (ex: 11:00 America/Sao_Paulo)
     * 2. Calcular targetTime = agora + antecedência (ex: 11:00 + 120min = 13:00)
     * 3. Buscar agendamentos com data/hora na janela [targetTime - 1min, targetTime + 1min]
     * 4. Para cada agendamento encontrado, verificar via Instant (UTC) que realmente
     *    está dentro da janela correta antes de enviar.
     *
     * @param tenant Entidade do tenant
     * @return Quantidade de lembretes enviados
     */
    private int processRemindersForTenant(TenantEntity tenant) {
        // Recarrega tenant do banco para garantir dados atualizados
        TenantEntity freshTenant = tenantService.getTenantByKey(tenant.getTenantKey());
        String tenantKey = freshTenant.getTenantKey();
        int minutosAntecedencia = freshTenant.getTempoLembreteMinutos();

        // Obtém "agora" no timezone do tenant — PONTO CRÍTICO
        ZonedDateTime nowZoned = tenantDateTimeService.now(tenantKey);
        LocalDateTime nowTenant = nowZoned.toLocalDateTime();
        ZoneId tenantZone = nowZoned.getZone();

        log.info("🔍 Tenant '{}': timezone={}, agora_tenant={}, agora_utc={}, antecedencia={} min ({} h)",
                tenantKey,
                tenantZone,
                nowTenant.format(DATE_TIME_FORMATTER),
                nowZoned.toInstant(),
                minutosAntecedencia,
                String.format("%.2f", minutosAntecedencia / 60.0));

        // Calcula o horário alvo: agendamentos que devem receber lembrete agora
        // Ex: agora_tenant=11:00, antecedência=120min → targetTime=13:00 (hora local do tenant)
        LocalDateTime targetTime = nowTenant.plusMinutes(minutosAntecedencia);

        // Janela de tolerância: ±1 minuto (cobre o intervalo entre execuções do scheduler)
        LocalDateTime windowStart = targetTime.minusMinutes(1);
        LocalDateTime windowEnd = targetTime.plusMinutes(1);

        log.info("📋 Tenant '{}': buscando agendamentos na janela_local [{} — {}] (target={})",
                tenantKey,
                windowStart.format(DATE_TIME_FORMATTER),
                windowEnd.format(DATE_TIME_FORMATTER),
                targetTime.format(DATE_TIME_FORMATTER));

        List<AppointmentsEntity> appointments = findAppointmentsToRemind(
                tenantKey, windowStart, windowEnd
        );

        log.info("📋 Tenant '{}': {} agendamento(s) candidatos a lembrete", tenantKey, appointments.size());

        int remindersSent = 0;
        for (AppointmentsEntity appointment : appointments) {
            if (sendReminderForAppointment(appointment, tenantZone, nowZoned.toInstant(), minutosAntecedencia)) {
                remindersSent++;
            }
        }

        return remindersSent;
    }

    /**
     * Busca agendamentos que precisam de lembrete no período especificado.
     * Busca apenas agendamentos com reminderSent = false.
     */
    private List<AppointmentsEntity> findAppointmentsToRemind(String tenantId,
                                                               LocalDateTime windowStart,
                                                               LocalDateTime windowEnd) {
        return appointmentsRepository.findAppointmentsToRemind(
                tenantId,
                windowStart.toLocalDate(),
                windowStart.toLocalTime(),
                windowEnd.toLocalDate(),
                windowEnd.toLocalTime()
        );
    }

    /**
     * Envia lembrete para um agendamento específico, com verificação de segurança em UTC.
     *
     * Antes de enviar, converte o horário do agendamento para Instant (UTC) e verifica
     * que a diferença real até o agendamento está dentro de [antecedência - 2min, antecedência + 2min].
     * Isso previne envio antecipado mesmo em cenários de borda.
     *
     * Em caso de sucesso, marca reminderSent = true (idempotência).
     *
     * @param appointment         Agendamento candidato
     * @param tenantZone          Timezone do tenant
     * @param nowInstant          Instante atual (UTC)
     * @param minutosAntecedencia Antecedência configurada em minutos
     * @return true se o lembrete foi enviado com sucesso
     */
    private boolean sendReminderForAppointment(AppointmentsEntity appointment,
                                                ZoneId tenantZone,
                                                Instant nowInstant,
                                                int minutosAntecedencia) {
        try {
            // Converte agendamento para Instant absoluto (UTC) — fonte da verdade
            LocalDateTime appointmentLocalDt = LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
            ZonedDateTime appointmentZoned = appointmentLocalDt.atZone(tenantZone);
            Instant appointmentInstant = appointmentZoned.toInstant();

            // Calcula diferença real em minutos (UTC vs UTC — sem ambiguidade)
            long diffMs = Duration.between(nowInstant, appointmentInstant).toMillis();
            long diffMinutos = diffMs / 60_000;
            double diffHoras = diffMinutos / 60.0;

            // Verificação de segurança: garante que a diferença está dentro da janela esperada
            long toleranciaMinutos = 2;
            long minEsperado = minutosAntecedencia - toleranciaMinutos;
            long maxEsperado = minutosAntecedencia + toleranciaMinutos;

            if (diffMinutos < minEsperado || diffMinutos > maxEsperado) {
                log.warn("⚠️ BLOQUEADO: Lembrete para {} seria enviado fora da janela! " +
                         "diff_real={}min, esperado=[{}-{}]min | " +
                         "appointment_utc={}, now_utc={}, tenant_zone={}",
                        appointment.getUserName(),
                        diffMinutos, minEsperado, maxEsperado,
                        appointmentInstant, nowInstant, tenantZone);
                return false;
            }

            // Logs estruturados para auditoria
            log.info("  ➡️  Enviando lembrete: user={} | scheduled_at_local={} {} | " +
                     "scheduled_at_utc={} | now_utc={} | diff_real={}min ({} h) | " +
                     "antecedencia_config={}min | tenant_zone={}",
                    appointment.getUserName(),
                    appointment.getDate().format(DATE_FORMATTER),
                    appointment.getStartTime().format(TIME_FORMATTER),
                    appointmentInstant,
                    nowInstant,
                    diffMinutos,
                    String.format("%.2f", diffHoras),
                    minutosAntecedencia,
                    tenantZone);

            whatsappService.enviarLembrete(appointment);

            appointment.setReminderSent(true);
            appointmentsRepository.save(appointment);

            log.info("  ✅ Lembrete enviado com sucesso para {} | appointment_id={}",
                    appointment.getUserName(), appointment.getId());
            return true;
        } catch (Exception e) {
            log.error("  ❌ Erro ao enviar lembrete para {} (appointment_id={}): {}",
                    appointment.getUserName(), appointment.getId(), e.getMessage(), e);
            return false;
        }
    }
}

