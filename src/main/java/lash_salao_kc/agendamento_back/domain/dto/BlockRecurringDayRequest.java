package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockRecurringDayRequest {

    @NotNull(message = "Tenant ID é obrigatório")
    private String tenantId;

    @NotNull(message = "Dia da semana é obrigatório")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Motivo é obrigatório")
    private String reason;
}

