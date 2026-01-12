package lash_salao_kc.agendamento_back.controller;

import lash_salao_kc.agendamento_back.config.TenantContext;

/**
 * Classe base abstrata para os controllers REST da aplicação.
 * Centraliza operações comuns relacionadas a multi-tenancy.
 *
 * IMPORTANTE: O TenantInterceptor já injeta o tenant no contexto antes dos controllers.
 * Os controllers só precisam usar getTenantFromContext() para obter o tenant.
 *
 * Todos os controllers que necessitam gerenciar multi-tenancy devem estender esta classe.
 */
public abstract class BaseController {

    /**
     * Obtém o tenant ID do contexto da requisição atual.
     * O TenantInterceptor já configurou este valor antes do controller ser chamado.
     *
     * @return Tenant ID da requisição atual
     */
    protected String getTenantFromContext() {
        return TenantContext.getTenantId();
    }

    /**
     * @deprecated Use getTenantFromContext() ao invés disso.
     * O TenantInterceptor já normaliza e injeta o tenant automaticamente.
     */
    @Deprecated
    protected String normalizeTenantId(String tenantId) {
        return tenantId.toLowerCase().trim();
    }

    /**
     * @deprecated Não é mais necessário.
     * O TenantInterceptor já configura o contexto automaticamente.
     */
    @Deprecated
    protected void setTenantContext(String tenantId) {
        TenantContext.setTenantId(tenantId);
    }
}

