package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAppointmentsByDateRequest {

    @NotNull(message = "Tenant ID é obrigatório")
    private String tenantId;

    private LocalDate date; // Optional - if null, returns all appointments
}

