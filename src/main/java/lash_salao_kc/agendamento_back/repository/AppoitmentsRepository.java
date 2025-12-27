package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppoitmentsRepository extends JpaRepository<AppointmentsEntity, UUID> {

    @Query("""
    SELECT a FROM AppointmentsEntity a
    WHERE a.reminderSent = false
      AND (a.date > :nowDate OR (a.date = :nowDate AND a.startTime >= :nowTime))
      AND (a.date < :limitDate OR (a.date = :limitDate AND a.startTime <= :limitTime))
""")
    List<AppointmentsEntity> findAppointmentsToRemind(
            @Param("nowDate") java.time.LocalDate nowDate,
            @Param("nowTime") java.time.LocalTime nowTime,
            @Param("limitDate") java.time.LocalDate limitDate,
            @Param("limitTime") java.time.LocalTime limitTime
    );
}
