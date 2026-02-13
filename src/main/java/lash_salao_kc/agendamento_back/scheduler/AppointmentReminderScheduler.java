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
        LocalDateTime limit = now.plusMinutes(minutosAntecedencia);

        log.info("📋 Tenant '{}': buscando agendamentos entre {} e {} ({} minutos de antecedência)",
                freshTenant.getTenantKey(),
                now.format(DATE_TIME_FORMATTER),
                limit.format(DATE_TIME_FORMATTER),
                minutosAntecedencia);

        List<AppointmentsEntity> appointments = findAppointmentsToRemind(freshTenant.getTenantKey(), now, limit);

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
    private List<AppointmentsEntity> findAppointmentsToRemind(String tenantId, LocalDateTime now, LocalDateTime limit) {
        return appointmentsRepository.findAppointmentsToRemind(
                tenantId,
                now.toLocalDate(),
                now.toLocalTime(),
                limit.toLocalDate(),
                limit.toLocalTime()
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
            log.info("  ➡️  Enviando lembrete para: {} | Data: {} às {}",
                appointment.getUserName(),
                appointment.getDate().format(DATE_FORMATTER),
                appointment.getStartTime().format(TIME_FORMATTER));

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

