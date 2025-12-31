package lash_salao_kc.agendamento_back.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Serviço para gerenciar os tenants (clientes) do sistema
 * Em produção, isso pode vir de um banco de dados ou configuração
 */
@Service
public class TenantService {

    /**
     * Retorna lista de todos os tenants ativos no sistema
     * Em produção, pode buscar de uma tabela tb_tenants
     * Por enquanto, retorna uma lista fixa com os dois clientes: KC e MJS
     */
    public List<String> getAllActiveTenants() {
        // TODO: Em produção, buscar de uma tabela de tenants
        // Por exemplo: SELECT tenant_id FROM tb_tenants WHERE active = true
        return Arrays.asList("kc", "mjs");
    }
}

