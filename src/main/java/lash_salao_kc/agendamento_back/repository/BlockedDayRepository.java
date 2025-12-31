package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.BlockedDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedDayRepository extends JpaRepository<BlockedDayEntity, UUID> {

    /**
     * Busca bloqueio por data específica e tenant
     */
    Optional<BlockedDayEntity> findByTenantIdAndSpecificDate(String tenantId, LocalDate date);

    /**
     * Busca bloqueio recorrente por dia da semana e tenant
     */
    Optional<BlockedDayEntity> findByTenantIdAndDayOfWeekAndRecurring(String tenantId, DayOfWeek dayOfWeek, boolean recurring);

    /**
     * Lista todos os bloqueios recorrentes (dias da semana) de um tenant
     */
    List<BlockedDayEntity> findByTenantIdAndRecurring(String tenantId, boolean recurring);

    /**
     * Lista todos os bloqueios de um tenant
     */
    List<BlockedDayEntity> findByTenantId(String tenantId);
}

