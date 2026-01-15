package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação e atualização de tenants.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank(message = "Chave do tenant é obrigatória")
    @Pattern(regexp = "^[a-z0-9\\-_]+$", message = "Chave deve conter apenas letras minúsculas, números, hífens e underscores")
    private String tenantKey;

    @NotBlank(message = "Nome comercial é obrigatório")
    private String businessName;

    @Email(message = "Email inválido")
    private String contactEmail;

    private String contactPhone;
}

