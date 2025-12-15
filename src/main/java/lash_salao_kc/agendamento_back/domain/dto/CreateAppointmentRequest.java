package lash_salao_kc.agendamento_back.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO para requisição de criar agendamento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "ID do serviço é obrigatório")
    private UUID serviceId;

    @NotNull(message = "Data do agendamento é obrigatória")
    private LocalDate date;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Nome do usuário é obrigatório")
    private String userName;

    @NotNull(message = "Número do usuário é obrigatório")
    private String userPhone;
}

