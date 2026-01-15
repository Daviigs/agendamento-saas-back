package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para gerenciar profissionais vinculados aos tenants.
 */
@Repository
public interface ProfessionalRepository extends JpaRepository<ProfessionalEntity, UUID> {

    /**
     * Busca todos os profissionais de um tenant específico.
     *
     * @param tenantId ID do tenant
     * @return Lista de profissionais do tenant
     */
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.tenant.id = :tenantId")
    List<ProfessionalEntity> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Busca profissionais ativos de um tenant.
     *
     * @param tenantId ID do tenant
     * @return Lista de profissionais ativos
     */
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.tenant.id = :tenantId AND p.active = true")
    List<ProfessionalEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Busca um profissional específico de um tenant.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return Optional contendo o profissional se pertencer ao tenant
     */
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.id = :professionalId AND p.tenant.id = :tenantId")
    Optional<ProfessionalEntity> findByIdAndTenantId(
            @Param("professionalId") UUID professionalId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Verifica se um profissional pertence a um tenant.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return true se o profissional pertence ao tenant
     */
    @Query("SELECT COUNT(p) > 0 FROM ProfessionalEntity p WHERE p.id = :professionalId AND p.tenant.id = :tenantId")
    boolean existsByIdAndTenantId(
            @Param("professionalId") UUID professionalId,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Busca um profissional ativo de um tenant específico.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return Optional contendo o profissional ativo
     */
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.id = :professionalId AND p.tenant.id = :tenantId AND p.active = true")
    Optional<ProfessionalEntity> findActiveByIdAndTenantId(
            @Param("professionalId") UUID professionalId,
            @Param("tenantId") UUID tenantId
    );
}

