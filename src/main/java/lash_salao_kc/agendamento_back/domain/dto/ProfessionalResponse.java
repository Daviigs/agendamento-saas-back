package lash_salao_kc.agendamento_back.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de resposta para profissional (evita lazy loading issues).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionalResponse {

    private UUID id;
    private UUID tenantId;
    private String professionalName;
    private String professionalEmail;
    private String professionalPhone;
    private Boolean active;
}

