package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayEntity;
import lash_salao_kc.agendamento_back.repository.BlockedDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockedDayService {

    private final BlockedDayRepository blockedDayRepository;

    /**
     * Verifica se uma data específica está bloqueada
     * Considera tanto bloqueios de data específica quanto bloqueios recorrentes por dia da semana
     */
    public boolean isDateBlocked(LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        // Verifica se há bloqueio para a data específica
        if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, date).isPresent()) {
            return true;
        }

        // Verifica se há bloqueio recorrente para o dia da semana
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return blockedDayRepository.findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true).isPresent();
    }

    /**
     * Bloqueia uma data específica (ex: 25/12/2025 - Natal)
     */
    @Transactional
    public BlockedDayEntity blockSpecificDate(LocalDate date, String reason) {
        String tenantId = TenantContext.getTenantId();

        // Verifica se já existe bloqueio para essa data
        if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, date).isPresent()) {
            throw new RuntimeException("Esta data já está bloqueada");
        }

        BlockedDayEntity blockedDay = new BlockedDayEntity();
        blockedDay.setTenantId(tenantId);
        blockedDay.setSpecificDate(date);
        blockedDay.setReason(reason);
        blockedDay.setRecurring(false);
        blockedDay.setDayOfWeek(null);

        return blockedDayRepository.save(blockedDay);
    }

    /**
     * Bloqueia um dia da semana recorrente (ex: todo domingo)
     */
    @Transactional
    public BlockedDayEntity blockRecurringDayOfWeek(DayOfWeek dayOfWeek, String reason) {
        String tenantId = TenantContext.getTenantId();

        // Verifica se já existe bloqueio para esse dia da semana
        if (blockedDayRepository.findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true).isPresent()) {
            throw new RuntimeException("Este dia da semana já está bloqueado");
        }

        BlockedDayEntity blockedDay = new BlockedDayEntity();
        blockedDay.setTenantId(tenantId);
        blockedDay.setDayOfWeek(dayOfWeek);
        blockedDay.setReason(reason);
        blockedDay.setRecurring(true);
        blockedDay.setSpecificDate(null);

        return blockedDayRepository.save(blockedDay);
    }

    /**
     * Remove um bloqueio por ID
     */
    @Transactional
    public void unblockDay(UUID blockedDayId) {
        BlockedDayEntity blockedDay = blockedDayRepository.findById(blockedDayId)
                .orElseThrow(() -> new RuntimeException("Bloqueio não encontrado com ID: " + blockedDayId));

        blockedDayRepository.delete(blockedDay);
    }

    /**
     * Lista todos os bloqueios
     */
    public List<BlockedDayEntity> getAllBlockedDays() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantId(tenantId);
    }

    /**
     * Lista apenas bloqueios de datas específicas
     */
    public List<BlockedDayEntity> getSpecificBlockedDates() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantIdAndRecurring(tenantId, false);
    }

    /**
     * Lista apenas bloqueios recorrentes (dias da semana)
     */
    public List<BlockedDayEntity> getRecurringBlockedDays() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantIdAndRecurring(tenantId, true);
    }

    /**
     * Retorna lista de datas disponíveis (não bloqueadas) dentro de um período
     * Útil para exibir calendário com dias disponíveis
     *
     * @param startDate Data inicial do período
     * @param endDate Data final do período
     * @return Lista de datas disponíveis (não bloqueadas)
     */
    public List<LocalDate> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> availableDates = new java.util.ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (!isDateBlocked(currentDate)) {
                availableDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return availableDates;
    }
}

