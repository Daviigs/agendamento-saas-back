package lash_salao_kc.agendamento_back.exception;

/**
 * Exceção lançada quando um recurso não é encontrado no sistema.
 * Utilizada para entidades como Agendamento, Serviço, etc.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s não encontrado(a) com identificador: %s", resourceName, identifier));
    }
}


