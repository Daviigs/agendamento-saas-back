package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO para configuração de horário de trabalho de um tenant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantWorkingHoursRequest {

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    private LocalTime endTime;

    private Integer slotIntervalMinutes = 30;
}

