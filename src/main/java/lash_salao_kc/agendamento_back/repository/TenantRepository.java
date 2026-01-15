package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para gerenciar tenants (salões/clientes) do sistema.
 */
@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * Busca um tenant pela sua chave única (usada no header X-Tenant-Id).
     *
     * @param tenantKey Chave do tenant (ex: "kc", "mjs")
     * @return Optional contendo o tenant se encontrado
     */
    Optional<TenantEntity> findByTenantKey(String tenantKey);

    /**
     * Verifica se existe um tenant com a chave especificada.
     *
     * @param tenantKey Chave do tenant
     * @return true se existe
     */
    boolean existsByTenantKey(String tenantKey);

    /**
     * Busca apenas tenants ativos.
     *
     * @return Lista de tenants ativos
     */
    List<TenantEntity> findByActiveTrue();

    /**
     * Busca um tenant ativo pela chave.
     *
     * @param tenantKey Chave do tenant
     * @return Optional contendo o tenant ativo se encontrado
     */
    Optional<TenantEntity> findByTenantKeyAndActiveTrue(String tenantKey);
}

