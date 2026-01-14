package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável por calcular os horários disponíveis para agendamento.
 * Considera:
 * - Horário de trabalho do profissional (tenant)
 * - Bloqueios de horários específicos
 * - Bloqueios recorrentes
 * - Agendamentos já existentes
 * - Bloqueios de dias inteiros (via BlockedDayService)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailableTimeSlotsService {

    private final TenantWorkingHoursService workingHoursService;
    private final BlockedTimeSlotService blockedTimeSlotService;
    private final BlockedDayService blockedDayService;
    private final AppointmentsRepository appointmentsRepository;

    /**
     * Retorna todos os horários disponíveis para agendamento em uma data específica.
     *
     * Considera:
     * - Se o dia está bloqueado completamente
     * - Horário de trabalho do tenant
     * - Bloqueios de horários
     * - Agendamentos existentes
     *
     * @param date     Data para consulta
     * @param tenantId ID do profissional (se null, usa o tenant do contexto)
     * @return Lista de horários disponíveis
     */
    public List<LocalTime> getAvailableTimeSlots(LocalDate date, String tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }

        log.info("Calculando horários disponíveis para {} - tenant: {}", date, tenantId);

        // Verifica se o dia inteiro está bloqueado
        if (blockedDayService.isDateBlocked(date)) {
            log.info("Dia {} está completamente bloqueado", date);
            return new ArrayList<>();
        }

        // Obtém horário de trabalho do tenant
        TenantWorkingHoursEntity workingHours = workingHoursService.getWorkingHours(tenantId);

        // Gera todos os slots possíveis baseado no horário de trabalho
        List<LocalTime> allPossibleSlots = generateAllTimeSlots(workingHours);

        // Obtém bloqueios de horário para esta data
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getBlockedTimeSlotsForDate(date);

        // Obtém agendamentos existentes na data
        List<AppointmentsEntity> appointments = appointmentsRepository.findByTenantIdAndDate(tenantId, date);

        // Filtra slots disponíveis
        List<LocalTime> availableSlots = allPossibleSlots.stream()
                .filter(slot -> !isSlotBlocked(slot, blockedSlots))
                .filter(slot -> !isSlotOccupiedByAppointment(slot, appointments))
                .collect(Collectors.toList());

        log.info("Encontrados {} horários disponíveis de {} possíveis",
                availableSlots.size(), allPossibleSlots.size());

        return availableSlots;
    }

    /**
     * Sobrecarga do método usando o tenant do contexto.
     */
    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        return getAvailableTimeSlots(date, null);
    }

    /**
     * Gera todos os horários possíveis de agendamento baseado no horário de trabalho.
     *
     * @param workingHours Horário de trabalho configurado
     * @return Lista com todos os horários possíveis
     */
    private List<LocalTime> generateAllTimeSlots(TenantWorkingHoursEntity workingHours) {
        List<LocalTime> slots = new ArrayList<>();

        LocalTime startTime = workingHours.getStartTime();
        LocalTime endTime = workingHours.getEndTime();
        Integer intervalMinutes = workingHours.getSlotIntervalMinutes();

        LocalTime currentSlot = startTime;

        // Gera slots até que não seja mais possível iniciar um agendamento antes do fim do expediente
        // O último slot deve permitir pelo menos um agendamento mínimo (considerando o intervalo)
        LocalTime lastPossibleStart = endTime.minusMinutes(intervalMinutes);

        while (currentSlot.isBefore(lastPossibleStart) || currentSlot.equals(lastPossibleStart)) {
            slots.add(currentSlot);
            currentSlot = currentSlot.plusMinutes(intervalMinutes);
        }

        return slots;
    }

    /**
     * Verifica se um slot está bloqueado por algum bloqueio de horário.
     *
     * @param slot         Horário a verificar
     * @param blockedSlots Lista de bloqueios ativos
     * @return true se o slot está bloqueado
     */
    private boolean isSlotBlocked(LocalTime slot, List<BlockedTimeSlotEntity> blockedSlots) {
        return blockedSlots.stream()
                .anyMatch(block -> isTimeWithinBlock(slot, block.getStartTime(), block.getEndTime()));
    }

    /**
     * Verifica se um slot está ocupado por um agendamento existente.
     *
     * @param slot         Horário a verificar
     * @param appointments Lista de agendamentos na data
     * @return true se o slot está ocupado
     */
    private boolean isSlotOccupiedByAppointment(LocalTime slot, List<AppointmentsEntity> appointments) {
        return appointments.stream()
                .anyMatch(appointment -> isTimeInAppointmentRange(slot, appointment));
    }

    /**
     * Verifica se um horário está dentro de um intervalo bloqueado.
     */
    private boolean isTimeWithinBlock(LocalTime time, LocalTime blockStart, LocalTime blockEnd) {
        return !time.isBefore(blockStart) && time.isBefore(blockEnd);
    }

    /**
     * Verifica se um horário está dentro do range de um agendamento existente.
     */
    private boolean isTimeInAppointmentRange(LocalTime time, AppointmentsEntity appointment) {
        LocalTime start = appointment.getStartTime();
        LocalTime end = appointment.getEndTime();
        return (time.equals(start) || time.isAfter(start)) && time.isBefore(end);
    }

    /**
     * Verifica se um horário específico está disponível para agendamento.
     *
     * @param date      Data do agendamento
     * @param startTime Horário de início desejado
     * @param duration  Duração em minutos
     * @param tenantId  ID do profissional
     * @return true se o horário está disponível
     */
    public boolean isTimeSlotAvailable(LocalDate date, LocalTime startTime, int duration, String tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }

        // Verifica se o dia está bloqueado
        if (blockedDayService.isDateBlocked(date)) {
            return false;
        }

        LocalTime endTime = startTime.plusMinutes(duration);

        // Verifica se está dentro do horário de trabalho
        if (!workingHoursService.isIntervalWithinWorkingHours(startTime, endTime, tenantId)) {
            return false;
        }

        // Verifica se há bloqueio de horário
        if (blockedTimeSlotService.isIntervalBlocked(date, startTime, endTime)) {
            return false;
        }

        // Verifica se há conflito com agendamentos existentes
        List<AppointmentsEntity> appointments = appointmentsRepository.findByTenantIdAndDate(tenantId, date);
        for (AppointmentsEntity appointment : appointments) {
            if (hasTimeConflict(startTime, endTime, appointment.getStartTime(), appointment.getEndTime())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se há conflito entre dois intervalos de tempo.
     */
    private boolean hasTimeConflict(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Retorna informações sobre a disponibilidade de uma data.
     * Útil para exibir estatísticas no frontend.
     *
     * @param date     Data a verificar
     * @param tenantId ID do profissional
     * @return Objeto com informações de disponibilidade
     */
    public DateAvailabilityInfo getDateAvailabilityInfo(LocalDate date, String tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }

        if (blockedDayService.isDateBlocked(date)) {
            return new DateAvailabilityInfo(date, 0, 0, true, "Dia completamente bloqueado");
        }

        TenantWorkingHoursEntity workingHours = workingHoursService.getWorkingHours(tenantId);
        List<LocalTime> allSlots = generateAllTimeSlots(workingHours);
        List<LocalTime> availableSlots = getAvailableTimeSlots(date, tenantId);

        return new DateAvailabilityInfo(
                date,
                allSlots.size(),
                availableSlots.size(),
                false,
                null
        );
    }

    /**
     * Classe interna para retornar informações de disponibilidade.
     */
    public static class DateAvailabilityInfo {
        public final LocalDate date;
        public final int totalSlots;
        public final int availableSlots;
        public final boolean fullyBlocked;
        public final String blockReason;

        public DateAvailabilityInfo(LocalDate date, int totalSlots, int availableSlots,
                                     boolean fullyBlocked, String blockReason) {
            this.date = date;
            this.totalSlots = totalSlots;
            this.availableSlots = availableSlots;
            this.fullyBlocked = fullyBlocked;
            this.blockReason = blockReason;
        }

        public boolean isAvailable() {
            return !fullyBlocked && availableSlots > 0;
        }

        public int getOccupiedSlots() {
            return totalSlots - availableSlots;
        }

        public double getOccupancyRate() {
            if (totalSlots == 0) return 0.0;
            return (double) getOccupiedSlots() / totalSlots * 100;
        }
    }
}

