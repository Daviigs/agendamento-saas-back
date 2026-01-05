package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceWithIdRequest {

    @NotNull(message = "Tenant ID é obrigatório")
    private String tenantId;

    @NotNull(message = "ID do serviço é obrigatório")
    private String id;

    @NotNull(message = "Nome do serviço é obrigatório")
    private String name;

    @NotNull(message = "Duração é obrigatória")
    private Integer duration;

    @NotNull(message = "Preço é obrigatório")
    private Double price;
}

