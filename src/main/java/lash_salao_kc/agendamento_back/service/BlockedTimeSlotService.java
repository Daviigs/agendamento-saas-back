package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.DuplicateResourceException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.BlockedTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de bloqueios de horários específicos.
 * Permite bloquear intervalos de tempo em datas específicas ou de forma recorrente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedTimeSlotService {

    private final BlockedTimeSlotRepository blockedTimeSlotRepository;
    private final TenantWorkingHoursService workingHoursService;

    /**
     * Bloqueia um intervalo de horário em uma data específica.
     *
     * @param date      Data do bloqueio
     * @param startTime Horário de início do bloqueio
     * @param endTime   Horário de término do bloqueio
     * @param reason    Motivo do bloqueio
     * @return Bloqueio criado
     * @throws BusinessException          se os horários forem inválidos
     * @throws DuplicateResourceException se já existe conflito de horário
     */
    @Transactional
    public BlockedTimeSlotEntity blockSpecificTimeSlot(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String reason) {

        String tenantId = TenantContext.getTenantId();

        validateTimeInterval(startTime, endTime);
        validateWithinWorkingHours(startTime, endTime, tenantId);
        validateNoConflictOnSpecificDate(tenantId, date, startTime, endTime);

        BlockedTimeSlotEntity blockedSlot = new BlockedTimeSlotEntity();
        blockedSlot.setTenantId(tenantId);
        blockedSlot.setSpecificDate(date);
        blockedSlot.setStartTime(startTime);
        blockedSlot.setEndTime(endTime);
        blockedSlot.setReason(reason);
        blockedSlot.setRecurring(false);
        blockedSlot.setDayOfWeek(null);

        log.info("Bloqueando horário específico: {} de {} às {} - Motivo: {}",
                date, startTime, endTime, reason);

        return blockedTimeSlotRepository.save(blockedSlot);
    }

    /**
     * Bloqueia um intervalo de horário de forma recorrente em um dia da semana.
     *
     * @param dayOfWeek Dia da semana
     * @param startTime Horário de início do bloqueio
     * @param endTime   Horário de término do bloqueio
     * @param reason    Motivo do bloqueio
     * @return Bloqueio criado
     * @throws BusinessException          se os horários forem inválidos
     * @throws DuplicateResourceException se já existe conflito de horário
     */
    @Transactional
    public BlockedTimeSlotEntity blockRecurringTimeSlot(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String reason) {

        String tenantId = TenantContext.getTenantId();

        validateTimeInterval(startTime, endTime);
        validateWithinWorkingHours(startTime, endTime, tenantId);
        validateNoConflictOnRecurringDay(tenantId, dayOfWeek, startTime, endTime);

        BlockedTimeSlotEntity blockedSlot = new BlockedTimeSlotEntity();
        blockedSlot.setTenantId(tenantId);
        blockedSlot.setDayOfWeek(dayOfWeek);
        blockedSlot.setStartTime(startTime);
        blockedSlot.setEndTime(endTime);
        blockedSlot.setReason(reason);
        blockedSlot.setRecurring(true);
        blockedSlot.setSpecificDate(null);

        log.info("Bloqueando horário recorrente: {} de {} às {} - Motivo: {}",
                dayOfWeek, startTime, endTime, reason);

        return blockedTimeSlotRepository.save(blockedSlot);
    }

    /**
     * Remove um bloqueio de horário existente (desbloqueia).
     *
     * @param blockedSlotId ID do bloqueio a ser removido
     * @throws ResourceNotFoundException se o bloqueio não for encontrado
     */
    @Transactional
    public void unblockTimeSlot(UUID blockedSlotId) {
        BlockedTimeSlotEntity blockedSlot = blockedTimeSlotRepository.findById(blockedSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio de horário", blockedSlotId));

        String tenantId = TenantContext.getTenantId();

        // Validar que o bloqueio pertence ao tenant atual
        if (!blockedSlot.getTenantId().equals(tenantId)) {
            throw new BusinessException("Você não tem permissão para remover este bloqueio");
        }

        log.info("Removendo bloqueio de horário: {}", blockedSlotId);
        blockedTimeSlotRepository.delete(blockedSlot);
    }

    /**
     * Lista todos os bloqueios de horário do tenant atual.
     *
     * @return Lista de bloqueios
     */
    public List<BlockedTimeSlotEntity> getAllBlockedTimeSlots() {
        String tenantId = TenantContext.getTenantId();
        return blockedTimeSlotRepository.findByTenantId(tenantId);
    }

    /**
     * Lista bloqueios de horário para uma data específica.
     * Inclui tanto bloqueios específicos da data quanto bloqueios recorrentes do dia da semana.
     *
     * @param date Data a consultar
     * @return Lista de bloqueios ativos na data
     */
    public List<BlockedTimeSlotEntity> getBlockedTimeSlotsForDate(LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        // Busca bloqueios específicos da data
        List<BlockedTimeSlotEntity> specificBlocks = blockedTimeSlotRepository
                .findByTenantIdAndSpecificDate(tenantId, date);

        // Busca bloqueios recorrentes do dia da semana
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<BlockedTimeSlotEntity> recurringBlocks = blockedTimeSlotRepository
                .findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true);

        // Combina ambas as listas
        specificBlocks.addAll(recurringBlocks);
        return specificBlocks;
    }

    /**
     * Lista apenas bloqueios recorrentes do tenant atual.
     *
     * @return Lista de bloqueios recorrentes
     */
    public List<BlockedTimeSlotEntity> getRecurringBlockedTimeSlots() {
        String tenantId = TenantContext.getTenantId();
        return blockedTimeSlotRepository.findByTenantIdAndRecurring(tenantId, true);
    }

    /**
     * Lista apenas bloqueios de datas específicas do tenant atual.
     *
     * @return Lista de bloqueios específicos
     */
    public List<BlockedTimeSlotEntity> getSpecificBlockedTimeSlots() {
        String tenantId = TenantContext.getTenantId();
        return blockedTimeSlotRepository.findByTenantIdAndRecurring(tenantId, false);
    }

    /**
     * Verifica se um horário específico está bloqueado em uma data.
     *
     * @param date Data a verificar
     * @param time Horário a verificar
     * @return true se o horário está bloqueado
     */
    public boolean isTimeSlotBlocked(LocalDate date, LocalTime time) {
        List<BlockedTimeSlotEntity> blockedSlots = getBlockedTimeSlotsForDate(date);

        return blockedSlots.stream()
                .anyMatch(block -> isTimeWithinBlock(time, block.getStartTime(), block.getEndTime()));
    }

    /**
     * Verifica se um intervalo de tempo está bloqueado em uma data.
     *
     * @param date      Data a verificar
     * @param startTime Início do intervalo
     * @param endTime   Fim do intervalo
     * @return true se há algum bloqueio no intervalo
     */
    public boolean isIntervalBlocked(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<BlockedTimeSlotEntity> blockedSlots = getBlockedTimeSlotsForDate(date);

        return blockedSlots.stream()
                .anyMatch(block -> hasTimeOverlap(startTime, endTime, block.getStartTime(), block.getEndTime()));
    }

    // ===== MÉTODOS DE VALIDAÇÃO =====

    /**
     * Valida se o intervalo de tempo é válido.
     */
    private void validateTimeInterval(LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BusinessException("Horário de início deve ser antes do horário de término");
        }
    }

    /**
     * Valida se o intervalo está dentro do horário de trabalho do tenant.
     */
    private void validateWithinWorkingHours(LocalTime startTime, LocalTime endTime, String tenantId) {
        if (!workingHoursService.isIntervalWithinWorkingHours(startTime, endTime, tenantId)) {
            throw new BusinessException("O intervalo de bloqueio deve estar dentro do horário de trabalho configurado");
        }
    }

    /**
     * Valida se não há conflito com bloqueios existentes em uma data específica.
     */
    private void validateNoConflictOnSpecificDate(
            String tenantId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {

        List<BlockedTimeSlotEntity> conflicts = blockedTimeSlotRepository
                .findConflictingBlocksOnSpecificDate(tenantId, date, startTime, endTime);

        if (!conflicts.isEmpty()) {
            throw new DuplicateResourceException(
                    "Já existe um bloqueio nesta data que conflita com o horário informado");
        }
    }

    /**
     * Valida se não há conflito com bloqueios recorrentes existentes.
     */
    private void validateNoConflictOnRecurringDay(
            String tenantId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime) {

        List<BlockedTimeSlotEntity> conflicts = blockedTimeSlotRepository
                .findConflictingRecurringBlocks(tenantId, dayOfWeek, startTime, endTime);

        if (!conflicts.isEmpty()) {
            throw new DuplicateResourceException(
                    "Já existe um bloqueio recorrente neste dia da semana que conflita com o horário informado");
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Verifica se um horário está dentro de um intervalo bloqueado.
     */
    private boolean isTimeWithinBlock(LocalTime time, LocalTime blockStart, LocalTime blockEnd) {
        return !time.isBefore(blockStart) && time.isBefore(blockEnd);
    }

    /**
     * Verifica se dois intervalos de tempo se sobrepõem.
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}

