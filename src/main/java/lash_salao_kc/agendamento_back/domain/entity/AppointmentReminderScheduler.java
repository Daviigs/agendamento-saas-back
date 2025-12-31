package lash_salao_kc.agendamento_back.domain.entity;

import lash_salao_kc.agendamento_back.repository.AppoitmentsRepository;
import lash_salao_kc.agendamento_back.service.TenantService;
import lash_salao_kc.agendamento_back.service.WhatsappSerivce;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private final AppoitmentsRepository appointmentsRepository;
    private final WhatsappSerivce whatsappService;
    private final TenantService tenantService;

    @Scheduled(fixedRate = 60000) // roda a cada 60 segundos (1 minuto)
    @Transactional
    public void sendReminders() {
        logger.info("🔔 Iniciando verificação de lembretes...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusHours(2);

        logger.info("📅 Buscando agendamentos entre {} e {}",
            now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            limit.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Itera por todos os tenants ativos
        List<String> tenants = tenantService.getAllActiveTenants();
        logger.info("👥 Tenants ativos: {}", tenants);

        int totalReminders = 0;

        for (String tenantId : tenants) {
            // Busca agendamentos para lembrete deste tenant específico
            List<AppointmentsEntity> appointments =
                    appointmentsRepository.findAppointmentsToRemind(
                            tenantId,
                            now.toLocalDate(),
                            now.toLocalTime(),
                            limit.toLocalDate(),
                            limit.toLocalTime()
                    );

            logger.info("📋 Tenant '{}': {} agendamento(s) para lembrar", tenantId, appointments.size());

            for (AppointmentsEntity appointment : appointments) {
                try {
                    logger.info("  ➡️  Enviando lembrete para: {} | Data: {} às {}",
                        appointment.getUserName(),
                        appointment.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));

                    whatsappService.enviarLembrete(appointment);
                    appointment.setReminderSent(true);
                    appointmentsRepository.save(appointment); // ✅ SALVA O APPOINTMENT
                    totalReminders++;

                    logger.info("  ✅ Lembrete enviado com sucesso!");
                } catch (Exception e) {
                    logger.error("  ❌ Erro ao enviar lembrete para {}: {}",
                        appointment.getUserName(), e.getMessage());
                }
            }
        }

        logger.info("🎯 Total de lembretes enviados: {}", totalReminders);
    }
}

