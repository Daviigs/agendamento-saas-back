package lash_salao_kc.agendamento_back.exception;

import java.time.LocalTime;

/**
 * Exceção específica para conflitos de agendamento.
 * Lançada quando há tentativa de agendar em horário já ocupado.
 */
public class AppointmentConflictException extends BusinessException {

    public AppointmentConflictException(LocalTime startTime, LocalTime endTime,
                                       LocalTime existingStart, LocalTime existingEnd,
                                       String existingUserName) {
        super(String.format(
            "Horário selecionado (%s - %s) conflita com agendamento existente (%s - %s) de %s",
            startTime, endTime, existingStart, existingEnd, existingUserName
        ));
    }
}

