package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização de tenants.
 * Todos os campos são opcionais - apenas os campos enviados serão atualizados.
 * Não inclui tenantKey pois este campo é imutável.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    /**
     * Nome comercial do salão.
     * Opcional - se não enviado, mantém o valor atual.
     */
    private String businessName;

    /**
     * Email de contato.
     * Opcional - se não enviado, mantém o valor atual.
     */
    @Email(message = "Email inválido")
    private String contactEmail;

    /**
     * Telefone de contato.
     * Opcional - se não enviado, mantém o valor atual.
     */
    private String contactPhone;

    /**
     * Tempo em minutos de antecedência para envio de lembretes.
     * Valores aceitos: 1 a 1440 minutos (1 minuto a 24 horas)
     * Opcional - se não enviado, mantém o valor atual.
     */
    @Min(value = 1, message = "Tempo de lembrete deve ser no mínimo 1 minuto")
    @Max(value = 1440, message = "Tempo de lembrete deve ser no máximo 1440 minutos (24 horas)")
    private Integer tempoLembreteMinutos;
}


