package lash_salao_kc.agendamento_back.utils;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.springframework.stereotype.Component;

@Component
public class BuildMensagens {

    public String buildConfirmationMessage(AppointmentsEntity appointment) {
        return """
            Olá %s! 😊
            
            Seu agendamento foi confirmado:
            Serviço: %s
            Data: %s
            Horário: %s
            
            Obrigado!
            """.formatted(
                appointment.getUserName(),
                appointment.getService().getName(),
                appointment.getDate(),
                appointment.getStartTime()
        );
    }
}
