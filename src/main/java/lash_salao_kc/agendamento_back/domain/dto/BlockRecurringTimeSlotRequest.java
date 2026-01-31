package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO para bloqueio recorrente de intervalo de horário em um dia da semana.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockRecurringTimeSlotRequest {

    @NotNull(message = "ID do profissional é obrigatório")
    private UUID professionalId;

    @NotNull(message = "Dia da semana é obrigatório")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    private LocalTime endTime;

    @NotNull(message = "Motivo é obrigatório")
    private String reason;
}

