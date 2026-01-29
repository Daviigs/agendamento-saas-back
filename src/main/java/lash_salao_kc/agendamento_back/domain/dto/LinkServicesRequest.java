package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para vincular serviços a um profissional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkServicesRequest {

    /**
     * Lista de IDs dos serviços a serem vinculados ao profissional.
     */
    @NotNull(message = "Lista de serviços é obrigatória")
    private List<UUID> serviceIds;
}

