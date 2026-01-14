package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

