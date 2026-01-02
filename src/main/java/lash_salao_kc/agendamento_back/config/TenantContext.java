package lash_salao_kc.agendamento_back.config;

/**
 * Contexto para armazenar o Tenant (cliente) da requisição atual
 * Usa ThreadLocal para isolar o tenant por thread/requisição
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Define o tenant atual para a thread/requisição
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Retorna o tenant atual da thread/requisição
     * Retorna "default" se nenhum tenant foi definido (retrocompatibilidade)
     */
    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : "default";
    }

    /**
     * Limpa o tenant do contexto (importante para evitar memory leak)
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

