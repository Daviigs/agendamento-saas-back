package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedTimeSlotRepository extends JpaRepository<BlockedTimeSlotEntity, UUID> {

    /**
     * Busca bloqueios de horário para uma data específica e tenant.
     */
    List<BlockedTimeSlotEntity> findByTenantIdAndSpecificDate(String tenantId, LocalDate date);

    /**
     * Busca bloqueios recorrentes para um dia da semana e tenant.
     */
    List<BlockedTimeSlotEntity> findByTenantIdAndDayOfWeekAndRecurring(String tenantId, DayOfWeek dayOfWeek, boolean recurring);

    /**
     * Lista todos os bloqueios de um tenant (específicos e recorrentes).
     */
    List<BlockedTimeSlotEntity> findByTenantId(String tenantId);

    /**
     * Lista apenas bloqueios recorrentes de um tenant.
     */
    List<BlockedTimeSlotEntity> findByTenantIdAndRecurring(String tenantId, boolean recurring);

    /**
     * Verifica se existe conflito de horário em uma data específica.
     */
    @Query("SELECT b FROM BlockedTimeSlotEntity b WHERE b.tenantId = :tenantId " +
           "AND b.specificDate = :date " +
           "AND b.recurring = false " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<BlockedTimeSlotEntity> findConflictingBlocksOnSpecificDate(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Verifica se existe conflito de horário em um dia da semana recorrente.
     */
    @Query("SELECT b FROM BlockedTimeSlotEntity b WHERE b.tenantId = :tenantId " +
           "AND b.dayOfWeek = :dayOfWeek " +
           "AND b.recurring = true " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<BlockedTimeSlotEntity> findConflictingRecurringBlocks(
            @Param("tenantId") String tenantId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Busca bloqueios de horário para uma data específica de um profissional.
     */
    @Query("SELECT b FROM BlockedTimeSlotEntity b WHERE b.professional.id = :professionalId AND b.specificDate = :date AND b.recurring = false")
    List<BlockedTimeSlotEntity> findByProfessionalIdAndSpecificDate(
            @Param("professionalId") UUID professionalId,
            @Param("date") LocalDate date);

    /**
     * Busca bloqueios recorrentes para um dia da semana de um profissional.
     */
    @Query("SELECT b FROM BlockedTimeSlotEntity b WHERE b.professional.id = :professionalId AND b.dayOfWeek = :dayOfWeek AND b.recurring = :recurring")
    List<BlockedTimeSlotEntity> findByProfessionalIdAndDayOfWeekAndRecurring(
            @Param("professionalId") UUID professionalId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("recurring") boolean recurring);
}

