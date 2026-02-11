package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayEntity;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayExceptionEntity;
import lash_salao_kc.agendamento_back.exception.DuplicateResourceException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.BlockedDayRepository;
import lash_salao_kc.agendamento_back.repository.BlockedDayExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de dias bloqueados.
 * Permite bloquear datas específicas (ex: feriados) ou dias da semana recorrentes (ex: domingos).
 * Suporta exceções para liberar dias específicos de bloqueios recorrentes.
 *
 * Ordem de prioridade para validação:
 * 1. Dia bloqueado pontual (específico) - MAIOR PRIORIDADE
 * 2. Dia liberado por exceção
 * 3. Dia bloqueado recorrente
 * 4. Dia permitido (sem bloqueio)
 *
 * Essencial para controle de disponibilidade de agendamentos.
 */
@Service
@RequiredArgsConstructor
public class BlockedDayService {

    private final BlockedDayRepository blockedDayRepository;
    private final BlockedDayExceptionRepository blockedDayExceptionRepository;

    /**
     * Verifica se uma data específica está bloqueada para agendamentos.
     *
     * Ordem de prioridade:
     * 1. Dia bloqueado pontual (data específica) - SEMPRE bloqueia
     * 2. Dia liberado por exceção - Libera mesmo se houver bloqueio recorrente
     * 3. Dia bloqueado recorrente - Bloqueia se não houver exceção
     * 4. Dia normal - Permitido
     *
     * @param date Data a ser verificada
     * @return true se a data está bloqueada, false caso contrário
     */
    public boolean isDateBlocked(LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        // PRIORIDADE 1: Verifica bloqueio de data específica (sempre tem prioridade máxima)
        if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, date).isPresent()) {
            return true;
        }

        // PRIORIDADE 2: Verifica se existe exceção liberando esta data
        if (blockedDayExceptionRepository.findByTenantIdAndExceptionDate(tenantId, date).isPresent()) {
            return false; // Data liberada por exceção
        }

        // PRIORIDADE 3: Verifica bloqueio recorrente por dia da semana
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return blockedDayRepository.findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true).isPresent();
    }

    /**
     * Bloqueia uma data específica para agendamentos.
     * Útil para feriados, eventos especiais ou fechamentos pontuais.
     *
     * @param date   Data a ser bloqueada (ex: 25/12/2025)
     * @param reason Motivo do bloqueio (ex: "Natal")
     * @return Bloqueio criado
     * @throws DuplicateResourceException se a data já estiver bloqueada
     */
    @Transactional
    public BlockedDayEntity blockSpecificDate(LocalDate date, String reason) {
        String tenantId = TenantContext.getTenantId();

        if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, date).isPresent()) {
            throw new DuplicateResourceException("Esta data já está bloqueada");
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
     * Bloqueia um dia da semana de forma recorrente.
     * Útil para folgas semanais fixas (ex: todos os domingos).
     *
     * @param dayOfWeek Dia da semana a ser bloqueado
     * @param reason    Motivo do bloqueio (ex: "Folga semanal")
     * @return Bloqueio criado
     * @throws DuplicateResourceException se o dia da semana já estiver bloqueado
     */
    @Transactional
    public BlockedDayEntity blockRecurringDayOfWeek(DayOfWeek dayOfWeek, String reason) {
        String tenantId = TenantContext.getTenantId();

        if (blockedDayRepository.findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true).isPresent()) {
            throw new DuplicateResourceException("Este dia da semana já está bloqueado");
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
     * Remove um bloqueio existente.
     *
     * @param blockedDayId ID do bloqueio a ser removido
     * @throws ResourceNotFoundException se o bloqueio não for encontrado
     */
    @Transactional
    public void unblockDay(UUID blockedDayId) {
        BlockedDayEntity blockedDay = blockedDayRepository.findById(blockedDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio", blockedDayId));

        blockedDayRepository.delete(blockedDay);
    }

    /**
     * Lista todos os bloqueios (específicos e recorrentes) do tenant atual.
     *
     * @return Lista de todos os bloqueios
     */
    public List<BlockedDayEntity> getAllBlockedDays() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantId(tenantId);
    }

    /**
     * Lista apenas bloqueios de datas específicas do tenant atual.
     *
     * @return Lista de bloqueios de datas específicas
     */
    public List<BlockedDayEntity> getSpecificBlockedDates() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantIdAndRecurring(tenantId, false);
    }

    /**
     * Lista apenas bloqueios recorrentes (dias da semana) do tenant atual.
     *
     * @return Lista de bloqueios recorrentes
     */
    public List<BlockedDayEntity> getRecurringBlockedDays() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayRepository.findByTenantIdAndRecurring(tenantId, true);
    }

    /**
     * Retorna lista de datas disponíveis (não bloqueadas) dentro de um período.
     * Útil para exibir calendário com dias disponíveis para agendamento.
     *
     * @param startDate Data inicial do período
     * @param endDate   Data final do período
     * @return Lista de datas disponíveis ordenadas
     */
    public List<LocalDate> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> availableDates = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (!isDateBlocked(currentDate)) {
                availableDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return availableDates;
    }

    /**
     * Cria uma exceção para liberar uma data específica de um bloqueio recorrente.
     *
     * Exemplo: Todos os domingos são bloqueados, mas você quer trabalhar em um domingo específico.
     *
     * @param exceptionDate Data a ser liberada
     * @param reason Motivo da liberação (ex: "Trabalho extra", "Reposição")
     * @return Exceção criada
     * @throws DuplicateResourceException se já existe uma exceção para esta data
     */
    @Transactional
    public BlockedDayExceptionEntity createException(LocalDate exceptionDate, String reason) {
        String tenantId = TenantContext.getTenantId();

        // Verifica se já existe exceção para esta data
        if (blockedDayExceptionRepository.findByTenantIdAndExceptionDate(tenantId, exceptionDate).isPresent()) {
            throw new DuplicateResourceException("Já existe uma exceção para esta data");
        }

        // Verifica se a data é um bloqueio pontual específico
        if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, exceptionDate).isPresent()) {
            throw new DuplicateResourceException(
                    "Esta data possui um bloqueio específico. Remova o bloqueio específico ao invés de criar uma exceção.");
        }

        BlockedDayExceptionEntity exception = new BlockedDayExceptionEntity();
        exception.setTenantId(tenantId);
        exception.setExceptionDate(exceptionDate);
        exception.setReason(reason);

        return blockedDayExceptionRepository.save(exception);
    }

    /**
     * Lista todas as exceções do tenant atual.
     *
     * @return Lista de exceções
     */
    public List<BlockedDayExceptionEntity> getAllExceptions() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayExceptionRepository.findByTenantId(tenantId);
    }

    /**
     * Lista exceções futuras (a partir de hoje).
     *
     * @return Lista de exceções futuras
     */
    public List<BlockedDayExceptionEntity> getFutureExceptions() {
        String tenantId = TenantContext.getTenantId();
        return blockedDayExceptionRepository.findByTenantIdAndExceptionDateGreaterThanEqual(tenantId, LocalDate.now());
    }

    /**
     * Remove uma exceção existente.
     *
     * @param exceptionId ID da exceção a ser removida
     * @throws ResourceNotFoundException se a exceção não for encontrada
     */
    @Transactional
    public void deleteException(UUID exceptionId) {
        BlockedDayExceptionEntity exception = blockedDayExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exceção", exceptionId));

        blockedDayExceptionRepository.delete(exception);
    }
}




