package lash_salao_kc.agendamento_back.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de tenants (clientes multi-tenant).
 * Em um cenário de produção, os dados dos tenants devem vir de um banco de dados.
 *
 * Multi-tenancy permite que múltiplos clientes compartilhem a mesma aplicação
 * mantendo seus dados isolados.
 */
@Service
public class TenantService {

    /**
     * Retorna lista de todos os tenants ativos no sistema.
     *
     * NOTA: Implementação atual usa lista fixa. Em produção, buscar de tabela
     * de tenants no banco de dados com query:
     * SELECT tenant_id FROM tb_tenants WHERE active = true
     *
     * @return Lista de IDs dos tenants ativos
     */
    public List<String> getAllActiveTenants() {
        // TODO: Em produção, substituir por consulta ao banco de dados
        return Arrays.asList("kc", "mjs");
    }
}

