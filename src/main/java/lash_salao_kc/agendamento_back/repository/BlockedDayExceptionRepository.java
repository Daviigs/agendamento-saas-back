package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.BlockedDayExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedDayExceptionRepository extends JpaRepository<BlockedDayExceptionEntity, UUID> {

    /**
     * Busca exceção por data específica e tenant
     */
    Optional<BlockedDayExceptionEntity> findByTenantIdAndExceptionDate(String tenantId, LocalDate exceptionDate);

    /**
     * Lista todas as exceções de um tenant
     */
    List<BlockedDayExceptionEntity> findByTenantId(String tenantId);

    /**
     * Lista exceções futuras de um tenant
     */
    List<BlockedDayExceptionEntity> findByTenantIdAndExceptionDateGreaterThanEqual(String tenantId, LocalDate fromDate);
}

