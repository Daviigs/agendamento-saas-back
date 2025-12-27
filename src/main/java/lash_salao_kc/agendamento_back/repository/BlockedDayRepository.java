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
     * Busca bloqueio por data específica
     */
    Optional<BlockedDayEntity> findBySpecificDate(LocalDate date);

    /**
     * Busca bloqueio recorrente por dia da semana
     */
    Optional<BlockedDayEntity> findByDayOfWeekAndRecurring(DayOfWeek dayOfWeek, boolean recurring);

    /**
     * Lista todos os bloqueios recorrentes (dias da semana)
     */
    List<BlockedDayEntity> findByRecurring(boolean recurring);
}

