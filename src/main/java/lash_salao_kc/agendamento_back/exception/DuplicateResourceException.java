package lash_salao_kc.agendamento_back.exception;

/**
 * Exceção lançada quando há tentativa de criar um recurso duplicado.
 * Utilizada para evitar duplicação de bloqueios, agendamentos conflitantes, etc.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

