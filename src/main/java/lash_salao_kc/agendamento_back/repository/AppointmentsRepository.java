package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Busca agendamentos por profissional e data
     */
    @Query("SELECT a FROM AppointmentsEntity a WHERE a.professional.id = :professionalId AND a.date = :date")
    List<AppointmentsEntity> findByProfessionalIdAndDate(
            @Param("professionalId") UUID professionalId,
            @Param("date") LocalDate date
    );

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

    /**
     * Verifica se existe algum agendamento FUTURO que utiliza o serviço especificado.
     * Considera futuro: data maior que hoje OU data igual a hoje com horário maior ou igual ao atual.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM AppointmentsEntity a 
        JOIN a.services s 
        WHERE s.id = :serviceId
        AND (a.date > :currentDate OR (a.date = :currentDate AND a.startTime >= :currentTime))
    """)
    boolean existsFutureAppointmentsByServiceId(
            @Param("serviceId") UUID serviceId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") java.time.LocalTime currentTime
    );

    /**
     * Remove todas as associações de um serviço com agendamentos.
     * Usado quando vamos deletar um serviço que só tem agendamentos passados.
     */
    @Modifying
    @Query(value = "DELETE FROM tb_appointment_services WHERE service_id = :serviceId", nativeQuery = true)
    void removeServiceFromAppointments(@Param("serviceId") UUID serviceId);
}

