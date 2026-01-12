package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentsRepository extends JpaRepository<AppointmentsEntity, UUID> {

    /**
     * Busca agendamentos por tenant
     */
    List<AppointmentsEntity> findByTenantId(String tenantId);

    /**
     * Busca agendamentos por tenant e data
     */
    List<AppointmentsEntity> findByTenantIdAndDate(String tenantId, LocalDate date);

    /**
     * Busca agendamentos por tenant e telefone
     */
    List<AppointmentsEntity> findByTenantIdAndUserPhone(String tenantId, String userPhone);

    @Query("""
    SELECT a FROM AppointmentsEntity a
    WHERE a.tenantId = :tenantId
      AND a.reminderSent = false
      AND (a.date > :nowDate OR (a.date = :nowDate AND a.startTime >= :nowTime))
      AND (a.date < :limitDate OR (a.date = :limitDate AND a.startTime <= :limitTime))
""")
    List<AppointmentsEntity> findAppointmentsToRemind(
            @Param("tenantId") String tenantId,
            @Param("nowDate") LocalDate nowDate,
            @Param("nowTime") java.time.LocalTime nowTime,
            @Param("limitDate") LocalDate limitDate,
            @Param("limitTime") java.time.LocalTime limitTime
    );
}

