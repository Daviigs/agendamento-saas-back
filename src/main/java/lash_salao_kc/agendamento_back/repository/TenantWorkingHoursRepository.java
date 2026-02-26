package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantWorkingHoursRepository extends JpaRepository<TenantWorkingHoursEntity, UUID> {

    /**
     * Busca o horário de trabalho global do tenant (onde professional_id IS NULL).
     * Garante que retorna apenas o registro do tenant, ignorando registros legados
     * que possam ter professional_id preenchido.
     */
    @Query("SELECT w FROM TenantWorkingHoursEntity w WHERE w.tenantId = :tenantId AND w.professional IS NULL")
    Optional<TenantWorkingHoursEntity> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Verifica se existe configuração de horário global para um tenant (sem professional_id).
     */
    @Query("SELECT COUNT(w) > 0 FROM TenantWorkingHoursEntity w WHERE w.tenantId = :tenantId AND w.professional IS NULL")
    boolean existsByTenantId(@Param("tenantId") String tenantId);
}

