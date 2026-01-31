package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO para bloqueio de intervalo de horário em uma data específica.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockSpecificTimeSlotRequest {

    @NotNull(message = "ID do profissional é obrigatório")
    private UUID professionalId;

    @NotNull(message = "Data é obrigatória")
    private LocalDate date;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    private LocalTime endTime;

    @NotNull(message = "Motivo é obrigatório")
    private String reason;
}

