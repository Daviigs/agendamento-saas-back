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
     * Busca horário de trabalho por tenant ID.
     */
    Optional<TenantWorkingHoursEntity> findByTenantId(String tenantId);

    /**
     * Verifica se existe configuração de horário para um tenant.
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Busca horário de trabalho por profissional ID.
     */
    @Query("SELECT w FROM TenantWorkingHoursEntity w WHERE w.professional.id = :professionalId")
    Optional<TenantWorkingHoursEntity> findByProfessionalId(@Param("professionalId") UUID professionalId);
}

