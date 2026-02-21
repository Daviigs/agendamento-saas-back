package lash_salao_kc.agendamento_back.domain.enums;

/**
 * Enum que representa o método de pagamento utilizado no agendamento.
 * Permite rastreamento detalhado das formas de pagamento.
 */
public enum PaymentMethod {
    /**
     * Pagamento em dinheiro/espécie
     */
    CASH("Dinheiro"),

    /**
     * Pagamento via PIX
     */
    PIX("PIX"),

    /**
     * Pagamento com cartão (débito ou crédito)
     */
    CARD("Cartão");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

