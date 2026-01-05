package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAvailableDatesRequest {

    @NotNull(message = "Tenant ID é obrigatório")
    private String tenantId;

    @NotNull(message = "Data inicial é obrigatória")
    private LocalDate startDate;

    @NotNull(message = "Data final é obrigatória")
    private LocalDate endDate;
}

