package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lash_salao_kc.agendamento_back.domain.enums.PaymentMethod;
import lash_salao_kc.agendamento_back.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para atualização de informações de pagamento de um agendamento.
 *
 * IMPORTANTE: O valor pago (totalAmount) é sempre uma escolha do usuário.
 * - Se não informado, mantém o valor original calculado
 * - Se informado, usa o valor especificado (independente de ter desconto ou não)
 * - O desconto é calculado automaticamente: originalAmount - totalAmount
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequest {

    /**
     * Novo status do pagamento
     */
    @NotNull(message = "Status de pagamento é obrigatório")
    private PaymentStatus paymentStatus;

    /**
     * Método de pagamento utilizado (obrigatório se status for PAID)
     */
    private PaymentMethod paymentMethod;

    /**
     * Valor efetivamente pago - SEMPRE OPCIONAL, ESCOLHA DO USUÁRIO
     *
     * - Se NÃO informado: usa o valor original calculado (sem desconto)
     * - Se informado: usa este valor (com ou sem desconto, conforme desejado)
     *
     * Exemplos:
     * - null → usa originalAmount (sem alteração)
     * - 120.00 → aplica este valor (pode ter desconto)
     * - 150.00 → aplica este valor (sem desconto, igual ao original)
     * - 180.00 → aplica este valor (com acréscimo)
     *
     * O desconto é calculado automaticamente pelo sistema.
     */
    private BigDecimal totalAmount;
}

