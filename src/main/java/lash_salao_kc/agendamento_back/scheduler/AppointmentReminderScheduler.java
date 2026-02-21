package lash_salao_kc.agendamento_back.scheduler;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.service.TenantService;
import lash_salao_kc.agendamento_back.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler responsável pelo envio automático de lembretes de agendamentos.
 *
 * Funcionalidade:
 * - Executa a cada minuto
 * - Verifica agendamentos que ocorrerão no tempo configurado pelo tenant (padrão: 2 horas)
 * - Envia lembrete via WhatsApp para clientes
 * - Marca agendamento como "lembrete enviado" para evitar duplicação
 * - Processa todos os tenants do sistema
 * - Cada tenant pode ter seu próprio tempo de antecedência configurado
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long SCHEDULER_INTERVAL_MS = 60000; // 1 minuto

    private final AppointmentsRepository appointmentsRepository;
    private final WhatsappService whatsappService;
    private final TenantService tenantService;

    /**
     * Método agendado que executa a cada minuto.
     * Processa lembretes para todos os tenants ativos.
     */
    @Scheduled(fixedRate = SCHEDULER_INTERVAL_MS)
    @Transactional
    public void sendReminders() {
        log.info("🔔 Iniciando verificação de lembretes...");

        LocalDateTime now = LocalDateTime.now();

        List<String> tenantKeys = tenantService.getAllActiveTenants();
        log.info("👥 Tenants ativos: {}", tenantKeys);

        int totalReminders = 0;

        for (String tenantKey : tenantKeys) {
            try {
                TenantEntity tenant = tenantService.getTenantByKey(tenantKey);
                totalReminders += processRemindersForTenant(tenant, now);
            } catch (Exception e) {
                log.error("❌ Erro ao processar lembretes do tenant '{}': {}", tenantKey, e.getMessage());
            }
        }

        log.info("🎯 Total de lembretes enviados: {}", totalReminders);
    }

    /**
     * Processa lembretes de agendamentos de um tenant específico.
     *
     * @param tenant Entidade do tenant
     * @param now    Data/hora atual
     * @return Quantidade de lembretes enviados
     */
    @Transactional(readOnly = true)
    private int processRemindersForTenant(TenantEntity tenant, LocalDateTime now) {
        // Força recarregar tenant do banco para garantir valor atualizado
        TenantEntity freshTenant = tenantService.getTenantByKey(tenant.getTenantKey());
        int minutosAntecedencia = freshTenant.getTempoLembreteMinutos();

        // 🔍 LOG DETALHADO: Mostrar valor lido do banco
        log.info("🔍 Tenant '{}': tempoLembreteMinutos lido do banco = {} minutos ({} horas)",
                freshTenant.getTenantKey(),
                minutosAntecedencia,
                String.format("%.2f", minutosAntecedencia / 60.0));

        // 🔧 CORREÇÃO: Calculamos o horário FUTURO do agendamento
        // Se agora é 10:00 e antecedência é 120 min, buscamos agendamentos às 12:00
        LocalDateTime targetTime = now.plusMinutes(minutosAntecedencia);

        // Janela de tolerância: ±1 minuto (para não perder lembretes entre execuções do scheduler)
        LocalDateTime windowStart = targetTime.minusMinutes(1);
        LocalDateTime windowEnd = targetTime.plusMinutes(1);

        log.info("📋 Tenant '{}': buscando agendamentos que ocorrerão em {} minutos às {} (janela: {} a {})",
                freshTenant.getTenantKey(),
                minutosAntecedencia,
                targetTime.format(DATE_TIME_FORMATTER),
                windowStart.format(DATE_TIME_FORMATTER),
                windowEnd.format(DATE_TIME_FORMATTER));

        List<AppointmentsEntity> appointments = findAppointmentsToRemind(
                freshTenant.getTenantKey(),
                windowStart,
                windowEnd
        );

        log.info("📋 Tenant '{}': {} agendamento(s) para lembrar", freshTenant.getTenantKey(), appointments.size());

        int remindersSent = 0;
         for (AppointmentsEntity appointment : appointments) {
            if (sendReminderForAppointment(appointment)) {
                remindersSent++;
            }
        }

        return remindersSent;
    }

    /**
     * Busca agendamentos que precisam de lembrete no período especificado.
     * Busca apenas agendamentos que ainda não tiveram lembrete enviado.
     */
    private List<AppointmentsEntity> findAppointmentsToRemind(String tenantId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return appointmentsRepository.findAppointmentsToRemind(
                tenantId,
                windowStart.toLocalDate(),
                windowStart.toLocalTime(),
                windowEnd.toLocalDate(),
                windowEnd.toLocalTime()
        );
    }

    /**
     * Envia lembrete para um agendamento específico.
     * Em caso de sucesso, marca o agendamento como "lembrete enviado".
     *
     * @param appointment Agendamento para enviar lembrete
     * @return true se o lembrete foi enviado com sucesso
     */
    private boolean sendReminderForAppointment(AppointmentsEntity appointment) {
        try {
            LocalDateTime appointmentDateTime = LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
            LocalDateTime now = LocalDateTime.now();
            long minutosAntes = java.time.Duration.between(now, appointmentDateTime).toMinutes();
            double horasAntes = minutosAntes / 60.0;

            log.info("  ➡️  Enviando lembrete para: {} | Agendamento: {} às {} | Antecedência: {} min ({} h)",
                appointment.getUserName(),
                appointment.getDate().format(DATE_FORMATTER),
                appointment.getStartTime().format(TIME_FORMATTER),
                minutosAntes,
                String.format("%.2f", horasAntes));

            whatsappService.enviarLembrete(appointment);

            appointment.setReminderSent(true);
            appointmentsRepository.save(appointment);

            log.info("  ✅ Lembrete enviado com sucesso!");
            return true;
        } catch (Exception e) {
            log.error("  ❌ Erro ao enviar lembrete para {}: {}",
                appointment.getUserName(), e.getMessage());
            return false;
        }
    }
}

