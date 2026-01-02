package lash_salao_kc.agendamento_back.domain.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para requisição de criar agendamento
 * Agora suporta múltiplos serviços
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotEmpty(message = "Pelo menos um serviço deve ser selecionado")
    private List<UUID> serviceIds;

    @NotNull(message = "Data do agendamento é obrigatória")
    private LocalDate date;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Nome do usuário é obrigatório")
    private String userName;

    @NotNull(message = "Número do usuário é obrigatório")
    private String userPhone;

    @NotNull(message = "ID do cliente é obrigatório")
    private String clienteId;
}

