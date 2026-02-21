package lash_salao_kc.agendamento_back.domain.enums;

/**
 * Enum que representa o status de pagamento de um agendamento.
 * Permite rastreamento financeiro dos serviços prestados.
 */
public enum PaymentStatus {
    /**
     * Pagamento pendente - serviço ainda não foi pago
     */
    PENDING("Pendente"),

    /**
     * Pagamento confirmado - serviço foi pago
     */
    PAID("Pago"),

    /**
     * Agendamento cancelado - não haverá pagamento
     */
    CANCELLED("Cancelado");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

