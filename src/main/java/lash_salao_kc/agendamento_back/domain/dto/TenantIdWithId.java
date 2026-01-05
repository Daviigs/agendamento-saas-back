package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantIdWithId {

    @NotNull(message = "Tenant ID é obrigatório")
    private String tenantId;

    @NotNull(message = "ID é obrigatório")
    private String id;
}

