package lash_salao_kc.agendamento_back.domain.entity;

import lash_salao_kc.agendamento_back.repository.AppoitmentsRepository;
import lash_salao_kc.agendamento_back.service.TenantService;
import lash_salao_kc.agendamento_back.service.WhatsappSerivce;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppoitmentsRepository appointmentsRepository;
    private final WhatsappSerivce whatsappService;
    private final TenantService tenantService;

    @Scheduled(fixedRate = 60000) // roda a cada 60 segundos (1 minuto)
    @Transactional
    public void sendReminders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusHours(2);

        // Itera por todos os tenants ativos
        List<String> tenants = tenantService.getAllActiveTenants();

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

            for (AppointmentsEntity appointment : appointments) {
                whatsappService.enviarLembrete(appointment);
                appointment.setReminderSent(true);
            }
        }
    }
}

