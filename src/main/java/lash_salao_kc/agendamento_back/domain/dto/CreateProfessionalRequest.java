package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de profissionais.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfessionalRequest {

    @NotBlank(message = "Nome do profissional é obrigatório")
    private String professionalName;

    @Email(message = "Email inválido")
    private String professionalEmail;

    private String professionalPhone;
}

