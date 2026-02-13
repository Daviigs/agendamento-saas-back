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
import java.util.UUID;
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
    private final ServicesService servicesService;
    private final TenantDateTimeService tenantDateTimeService;

    /**
     * Retorna todos os horários disponíveis para agendamento de um profissional específico.
     * Considera a duração dos serviços selecionados e bloqueios de horário.
     *
     * A flag horarioFlexivel determina o comportamento:
     * - false (Rígido): Descarta horários cujo término ultrapassaria bloqueios ou horário final
     * - true (Flexível): Permite que o término ultrapasse bloqueios e horário final
     *
     * @param professionalId ID do profissional
     * @param date Data para consulta
     * @param serviceIds Lista de IDs dos serviços (opcional)
     * @return Lista de horários disponíveis
     */
    public List<LocalTime> getAvailableTimeSlotsForProfessional(UUID professionalId, LocalDate date, List<UUID> serviceIds) {
        log.info("Calculando horários disponíveis para profissional {} na data {} com serviços: {}",
                professionalId, date, serviceIds);

        // Verifica se o dia inteiro está bloqueado
        if (blockedDayService.isDateBlocked(date)) {
            log.info("Dia {} está completamente bloqueado", date);
            return new ArrayList<>();
        }

        // Obtém horário de trabalho do profissional
        TenantWorkingHoursEntity workingHours = workingHoursService.getWorkingHoursByProfessional(professionalId);

        // Log informativo sobre o modo de horário
        boolean isFlexible = Boolean.TRUE.equals(workingHours.getHorarioFlexivel());
        log.info("Modo de horário do profissional {}: {} (horarioFlexivel={})",
                professionalId, isFlexible ? "FLEXÍVEL" : "RÍGIDO", isFlexible);

        // Calcula duração total dos serviços (se fornecidos)
        int totalDuration = 0;
        if (serviceIds != null && !serviceIds.isEmpty()) {
            totalDuration = calculateServicesDuration(serviceIds);
            log.info("Duração total dos serviços: {} minutos", totalDuration);
        }

        // Obtém agendamentos existentes deste profissional na data
        List<AppointmentsEntity> appointments = appointmentsRepository
                .findByProfessionalIdAndDate(professionalId, date);

        // Gera todos os slots possíveis baseado no horário de trabalho
        // INCLUI os horários de término dos agendamentos existentes como novos pontos de início
        List<LocalTime> allPossibleSlots = generateAllTimeSlotsWithAppointmentEndTimes(workingHours, appointments);

        // Obtém bloqueios de horário deste profissional para esta data
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService
                .getBlockedTimeSlotsForProfessionalAndDate(professionalId, date);

        log.info("Bloqueios encontrados para a data: {}", blockedSlots.size());
        blockedSlots.forEach(block ->
            log.info("  - Bloqueio: {} até {}", block.getStartTime(), block.getEndTime()));


        log.info("Agendamentos existentes: {}", appointments.size());

        // Filtra slots disponíveis
        final int serviceDuration = totalDuration;
        log.info("Iniciando filtragem de {} slots possíveis (duração do serviço: {} min)",
                allPossibleSlots.size(), serviceDuration);

        // Obtém tenantId do contexto para validação de horários passados
        String tenantId = TenantContext.getTenantId();

        List<LocalTime> availableSlots = allPossibleSlots.stream()
                .filter(slot -> !isSlotBlocked(slot, blockedSlots))
                // NOVA VALIDAÇÃO: Remove horários que já passaram (se a data for hoje)
                .filter(slot -> !isTimeSlotInPast(date, slot, tenantId))
                // NOVA VALIDAÇÃO: Considera a duração do serviço ao verificar conflitos com agendamentos
                .filter(slot -> {
                    if (serviceDuration > 0) {
                        // Verifica se o novo agendamento (slot + duração) conflitaria com agendamentos existentes
                        boolean wouldConflictWithExisting = wouldConflictWithAppointments(slot, serviceDuration, appointments);
                        if (wouldConflictWithExisting) {
                            log.debug("  ❌ Slot {} removido (conflitaria com agendamento existente)", slot);
                            return false;
                        }
                    } else {
                        // Sem duração, usa validação simples
                        if (isSlotOccupiedByAppointment(slot, appointments)) {
                            return false;
                        }
                    }
                    return true;
                })
                // REGRA: Se serviços foram informados, verifica se o horário final não ultrapassa bloqueios
                .filter(slot -> {
                    if (serviceDuration > 0) {
                        boolean wouldConflict = wouldEndTimeConflictWithBlockedSlots(slot, serviceDuration, blockedSlots, workingHours);
                        if (wouldConflict) {
                            log.debug("  ❌ Slot {} removido (terminaria em conflito com bloqueio)", slot);
                        } else {
                            log.debug("  ✅ Slot {} OK (termina às {})", slot, slot.plusMinutes(serviceDuration));
                        }
                        return !wouldConflict;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        log.info("Encontrados {} horários disponíveis de {} possíveis para profissional {}",
                availableSlots.size(), allPossibleSlots.size(), professionalId);

        return availableSlots;
    }

    /**
     * Retorna todos os horários disponíveis para agendamento de um profissional específico.
     * Método de compatibilidade sem serviceIds.
     *
     * @param professionalId ID do profissional
     * @param date Data para consulta
     * @return Lista de horários disponíveis
     */
    public List<LocalTime> getAvailableTimeSlotsForProfessional(UUID professionalId, LocalDate date) {
        return getAvailableTimeSlotsForProfessional(professionalId, date, null);
    }

    /**
     * Retorna todos os horários disponíveis para agendamento em uma data específica.
     *
     * Considera:
     * - Se o dia está bloqueado completamente
     * - Horário de trabalho do tenant
     * - Bloqueios de horários
     * - Agendamentos existentes
     * - Flag horarioFlexivel (não afeta este método diretamente, pois não considera duração)
     *
     * @param date     Data para consulta
     * @param tenantId ID do profissional (se null, usa o tenant do contexto)
     * @return Lista de horários disponíveis
     */
    public List<LocalTime> getAvailableTimeSlots(LocalDate date, String tenantId) {
        // Use final variable for lambda compatibility
        final String finalTenantId = (tenantId == null) ? TenantContext.getTenantId() : tenantId;

        log.info("Calculando horários disponíveis para {} - tenant: {}", date, finalTenantId);

        // Verifica se o dia inteiro está bloqueado
        if (blockedDayService.isDateBlocked(date)) {
            log.info("Dia {} está completamente bloqueado", date);
            return new ArrayList<>();
        }

        // Obtém horário de trabalho do tenant
        TenantWorkingHoursEntity workingHours = workingHoursService.getWorkingHours(finalTenantId);

        // Log informativo sobre o modo de horário
        boolean isFlexible = Boolean.TRUE.equals(workingHours.getHorarioFlexivel());
        log.info("Modo de horário: {} (horarioFlexivel={})",
                isFlexible ? "FLEXÍVEL" : "RÍGIDO", isFlexible);

        // Obtém agendamentos existentes na data
        List<AppointmentsEntity> appointments = appointmentsRepository.findByTenantIdAndDate(finalTenantId, date);

        // Gera todos os slots possíveis baseado no horário de trabalho
        // INCLUI os horários de término dos agendamentos existentes como novos pontos de início
        List<LocalTime> allPossibleSlots = generateAllTimeSlotsWithAppointmentEndTimes(workingHours, appointments);

        // Obtém bloqueios de horário para esta data
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getBlockedTimeSlotsForDate(date);


        // Filtra slots disponíveis
        List<LocalTime> availableSlots = allPossibleSlots.stream()
                .filter(slot -> !isSlotBlocked(slot, blockedSlots))
                .filter(slot -> !isTimeSlotInPast(date, slot, finalTenantId))
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
     * Gera todos os horários possíveis de agendamento baseado no horário de trabalho
     * E TAMBÉM INCLUI os horários de término dos agendamentos existentes.
     *
     * FUNCIONALIDADE CHAVE: Permite que novos agendamentos comecem exatamente quando outros terminam,
     * mesmo que o horário de término não esteja na grade fixa.
     *
     * Exemplo:
     * - Intervalo configurado: 30 minutos
     * - Grade fixa: 09:00, 09:30, 10:00, 10:30...
     * - Agendamento existente: 09:00 às 09:40 (duração 40 min)
     * - Resultado: 09:00, 09:30, 09:40, 10:00, 10:30...
     *               (09:40 é adicionado pois é quando o agendamento termina)
     *
     * @param workingHours Horário de trabalho configurado
     * @param appointments Agendamentos existentes na data
     * @return Lista com todos os horários possíveis (grade fixa + términos de agendamentos)
     */
    private List<LocalTime> generateAllTimeSlotsWithAppointmentEndTimes(
            TenantWorkingHoursEntity workingHours,
            List<AppointmentsEntity> appointments) {

        // 1. Gera a grade fixa baseada no intervalo configurado
        List<LocalTime> slots = generateAllTimeSlots(workingHours);

        // 2. Adiciona os horários de término dos agendamentos existentes
        if (appointments != null && !appointments.isEmpty()) {
            LocalTime workingEndTime = workingHours.getEndTime();

            for (AppointmentsEntity appointment : appointments) {
                LocalTime endTime = appointment.getEndTime();

                // Só adiciona se:
                // - O horário de término está dentro do expediente
                // - Ainda não existe na lista
                if (!endTime.isAfter(workingEndTime) && !slots.contains(endTime)) {
                    slots.add(endTime);
                    log.debug("➕ Adicionado horário {} (término do agendamento {})",
                            endTime, appointment.getStartTime());
                }
            }

            // 3. Ordena a lista final para manter a ordem cronológica
            slots.sort(LocalTime::compareTo);

            log.info("✅ Gerados {} horários possíveis ({} da grade fixa + {} de términos de agendamentos)",
                    slots.size(),
                    generateAllTimeSlots(workingHours).size(),
                    slots.size() - generateAllTimeSlots(workingHours).size());
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
     * Verifica se um horário (slot) está no passado considerando a data e o timezone do tenant.
     *
     * REGRAS:
     * - Se a data for anterior à data atual: retorna true (está no passado)
     * - Se a data for igual à data atual: retorna true se o horário já passou
     * - Se a data for futura: retorna false (nunca está no passado)
     *
     * @param date Data do slot
     * @param slot Horário do slot
     * @param tenantId ID do tenant
     * @return true se o slot está no passado
     */
    private boolean isTimeSlotInPast(LocalDate date, LocalTime slot, String tenantId) {
        // Verifica se a data está no passado
        if (tenantDateTimeService.isDateInPast(date, tenantId)) {
            log.debug("  ⏱️ Slot {} na data {} está no passado (data anterior)", slot, date);
            return true;
        }

        // Se a data é hoje, verifica se o horário já passou
        if (tenantDateTimeService.isToday(date, tenantId)) {
            boolean isPast = tenantDateTimeService.isInPast(date, slot, tenantId);
            if (isPast) {
                log.debug("  ⏱️ Slot {} na data {} está no passado (horário já passou)", slot, date);
            }
            return isPast;
        }

        // Data futura: nunca está no passado
        return false;
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
     * Considera a flag horarioFlexivel:
     * - false (Rígido): Horário de término não pode ultrapassar bloqueios ou horário final
     * - true (Flexível): Permite ultrapassar bloqueios e horário final
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

        // Obtém o horário de trabalho para verificar a flag horarioFlexivel
        TenantWorkingHoursEntity workingHours = workingHoursService.getWorkingHours(tenantId);
        boolean isFlexible = Boolean.TRUE.equals(workingHours.getHorarioFlexivel());

        // Modo RÍGIDO: Verifica se está dentro do horário de trabalho
        if (!isFlexible && !workingHoursService.isIntervalWithinWorkingHours(startTime, endTime, tenantId)) {
            return false;
        }

        // Modo RÍGIDO: Verifica se há bloqueio de horário
        if (!isFlexible && blockedTimeSlotService.isIntervalBlocked(date, startTime, endTime)) {
            return false;
        }

        // Modo FLEXÍVEL: Verifica apenas se o horário de INÍCIO não está bloqueado
        if (isFlexible) {
            // No modo flexível, apenas o ponto de início não pode estar em um bloqueio
            List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getBlockedTimeSlotsForDate(date);
            if (isSlotBlocked(startTime, blockedSlots)) {
                return false;
            }
        }

        // Verifica se há conflito com agendamentos existentes (sempre obrigatório)
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

    /**
     * Calcula a duração total dos serviços em minutos.
     *
     * @param serviceIds Lista de IDs dos serviços
     * @return Duração total em minutos
     */
    private int calculateServicesDuration(List<UUID> serviceIds) {
        int totalDuration = 0;
        for (UUID serviceId : serviceIds) {
            try {
                var service = servicesService.findById(serviceId);
                totalDuration += service.getDuration();
            } catch (Exception e) {
                log.warn("Erro ao buscar serviço {}: {}", serviceId, e.getMessage());
            }
        }
        return totalDuration;
    }

    /**
     * Verifica se o horário de término do atendimento (slot + duração) ultrapassaria ou coincidiria
     * com um horário bloqueado.
     *
     * REGRA DE NEGÓCIO:
     * - horarioFlexivel = false: Não deve exibir horários de início cujo horário final
     *   ultrapasse ou coincida com um horário bloqueado ou o horário final de funcionamento.
     *
     * - horarioFlexivel = true: Permite que agendamentos ultrapassem bloqueios e o horário final.
     *   Apenas verifica se o horário de INÍCIO está disponível.
     *
     * @param slot Horário de início proposto
     * @param duration Duração do serviço em minutos
     * @param blockedSlots Lista de bloqueios ativos
     * @param workingHours Horário de trabalho
     * @return true se haveria conflito (horário não deve ser exibido)
     */
    private boolean wouldEndTimeConflictWithBlockedSlots(
            LocalTime slot,
            int duration,
            List<BlockedTimeSlotEntity> blockedSlots,
            TenantWorkingHoursEntity workingHours) {

        LocalTime endTime = slot.plusMinutes(duration);

        // Se horário é FLEXÍVEL, permite ultrapassar bloqueios e horário final
        if (Boolean.TRUE.equals(workingHours.getHorarioFlexivel())) {
            log.debug("✅ Horário flexível ativo: Slot {} permitido (mesmo que termine às {} após expediente/bloqueios)",
                    slot, endTime);
            return false; // Não há conflito em modo flexível
        }

        // Modo RÍGIDO: Verifica se o horário de término ultrapassa o horário de trabalho
        if (endTime.isAfter(workingHours.getEndTime())) {
            log.debug("Horário {} + {} min resultaria em término após o expediente", slot, duration);
            return true;
        }

        // Modo RÍGIDO: Verifica se o horário de término coincide ou ultrapassa algum bloqueio
        for (BlockedTimeSlotEntity block : blockedSlots) {
            LocalTime blockStart = block.getStartTime();
            LocalTime blockEnd = block.getEndTime();

            // REGRA PRINCIPAL: Se o horário de término (slot + duração) for >= ao início do bloqueio
            // E o slot de início for < fim do bloqueio, então há conflito
            // Isso cobre todos os casos:
            // 1. Término coincide com início do bloqueio (ex: 11:30 + 30min = 12:00, bloqueio às 12:00)
            // 2. Término ultrapassa início do bloqueio (ex: 11:30 + 50min = 12:20, bloqueio às 12:00)
            // 3. Atendimento atravessa o bloqueio (ex: 11:00 + 100min = 12:40, bloqueio 12:00-13:00)

            if (!endTime.isBefore(blockStart) && slot.isBefore(blockEnd)) {
                log.debug("❌ BLOQUEADO: Slot {} + {} min terminaria às {} (bloqueio: {} - {})",
                        slot, duration, endTime, blockStart, blockEnd);
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se um novo agendamento (slot + duração) conflitaria com agendamentos existentes.
     *
     * REGRA CRÍTICA: Dois agendamentos conflitam se seus intervalos se sobrepõem.
     *
     * Conflito ocorre quando:
     * - Novo agendamento começa ANTES do fim de um existente E
     * - Novo agendamento termina DEPOIS do início de um existente
     *
     * Exemplos:
     * - Existente: 11:30-12:20
     * - Novo 11:20-12:10: CONFLITA (11:20 < 12:20 E 12:10 > 11:30) ✅
     * - Novo 11:00-11:30: NÃO CONFLITA (termina exatamente quando o outro começa)
     * - Novo 12:20-13:00: NÃO CONFLITA (começa exatamente quando o outro termina)
     *
     * @param slot Horário de início do novo agendamento
     * @param duration Duração do novo agendamento em minutos
     * @param appointments Lista de agendamentos existentes
     * @return true se haveria conflito
     */
    private boolean wouldConflictWithAppointments(LocalTime slot, int duration, List<AppointmentsEntity> appointments) {
        LocalTime newEndTime = slot.plusMinutes(duration);

        for (AppointmentsEntity existingAppointment : appointments) {
            LocalTime existingStart = existingAppointment.getStartTime();
            LocalTime existingEnd = existingAppointment.getEndTime();

            // Verifica se há sobreposição entre os intervalos
            // Conflito: novo.início < existente.fim E novo.fim > existente.início
            if (slot.isBefore(existingEnd) && newEndTime.isAfter(existingStart)) {
                log.debug("❌ CONFLITO: Novo agendamento {} - {} conflita com existente {} - {}",
                        slot, newEndTime, existingStart, existingEnd);
                return true;
            }
        }

        return false;
    }
}




