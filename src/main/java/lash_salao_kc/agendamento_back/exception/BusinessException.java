package lash_salao_kc.agendamento_back.exception;

/**
 * Exceção para erros de regras de negócio.
 * Utilizada quando uma operação não pode ser realizada devido a violação de regras.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

