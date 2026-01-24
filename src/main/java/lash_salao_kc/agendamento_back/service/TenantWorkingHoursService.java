package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
    import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.repository.ProfessionalRepository;
import lash_salao_kc.agendamento_back.repository.TenantWorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de horários de trabalho dos tenants (profissionais).
 * Cada tenant pode ter seu próprio horário de funcionamento personalizado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWorkingHoursService {

    private final TenantWorkingHoursRepository workingHoursRepository;
    private final ProfessionalRepository professionalRepository;
    private final TenantService tenantService;

    // Horários padrão caso o tenant não tenha configuração específica
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final Integer DEFAULT_SLOT_INTERVAL = 30;

    /**
     * Obtém o horário de trabalho de um tenant.
     * Se não existir configuração, retorna horários padrão.
     *
     * @param tenantId ID do tenant
     * @return Horário de trabalho configurado ou padrão
     */
    public TenantWorkingHoursEntity getWorkingHours(String tenantId) {
        return workingHoursRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultWorkingHours(tenantId));
    }

    /**
     * Obtém o horário de trabalho do tenant atual (do contexto).
     *
     * @return Horário de trabalho do tenant atual
     */
    public TenantWorkingHoursEntity getCurrentTenantWorkingHours() {
        String tenantId = TenantContext.getTenantId();
        return getWorkingHours(tenantId);
    }

    /**
     * Obtém o horário de trabalho de um profissional específico.
     * Se não existir configuração, retorna horários padrão.
     *
     * @param professionalId ID do profissional
     * @return Horário de trabalho configurado ou padrão
     */
    public TenantWorkingHoursEntity getWorkingHoursByProfessional(UUID professionalId) {
        return workingHoursRepository.findByProfessionalId(professionalId)
                .orElseGet(() -> {
                    String tenantId = TenantContext.getTenantId();
                    return createDefaultWorkingHours(tenantId);
                });
    }

    /**
     * Cria horário de trabalho padrão (não persiste no banco).
     */
    private TenantWorkingHoursEntity createDefaultWorkingHours(String tenantId) {
        TenantWorkingHoursEntity defaultHours = new TenantWorkingHoursEntity();
        defaultHours.setTenantId(tenantId);
        defaultHours.setStartTime(DEFAULT_START_TIME);
        defaultHours.setEndTime(DEFAULT_END_TIME);
        defaultHours.setSlotIntervalMinutes(DEFAULT_SLOT_INTERVAL);
        defaultHours.setActive(true);
        return defaultHours;
    }

    /**
     * Busca ou cria o primeiro profissional de um tenant.
     * Necessário porque TenantWorkingHours precisa estar associado a um professional.
     */
    private ProfessionalEntity getOrCreateProfessionalForTenant(String tenantId) {
        TenantEntity tenant = tenantService.getTenantByKey(tenantId);

        // Busca profissionais existentes do tenant
        List<ProfessionalEntity> professionals = professionalRepository.findByTenantId(tenant.getId());

        if (!professionals.isEmpty()) {
            return professionals.getFirst(); // Retorna o primeiro profissional
        }

        // Se não existe nenhum profissional, cria um profissional padrão
        log.warn("Tenant {} não possui profissionais. Criando profissional padrão.", tenantId);
        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setTenant(tenant);
        professional.setProfessionalName("Profissional Padrão - " + tenant.getBusinessName());
        professional.setProfessionalEmail(tenant.getContactEmail() != null ? tenant.getContactEmail() : "contato@" + tenantId + ".com");
        professional.setProfessionalPhone(tenant.getContactPhone() != null ? tenant.getContactPhone() : "00000000000");
        professional.setActive(true);

        return professionalRepository.save(professional);
    }

    /**
     * Configura ou atualiza o horário de trabalho de um tenant.
     *
     * @param startTime           Horário de início
     * @param endTime             Horário de término
     * @param slotIntervalMinutes Intervalo entre slots (minutos)
     * @return Configuração salva
     * @throws BusinessException se os horários forem inválidos
     */
    @Transactional
    public TenantWorkingHoursEntity configureWorkingHours(
            LocalTime startTime,
            LocalTime endTime,
            Integer slotIntervalMinutes) {

        String tenantId = TenantContext.getTenantId();

        validateWorkingHours(startTime, endTime, slotIntervalMinutes);

        Optional<TenantWorkingHoursEntity> existing = workingHoursRepository.findByTenantId(tenantId);

        if (existing.isPresent()) {
            // Atualiza configuração existente
            TenantWorkingHoursEntity workingHours = existing.get();
            workingHours.setStartTime(startTime);
            workingHours.setEndTime(endTime);
            workingHours.setSlotIntervalMinutes(slotIntervalMinutes);
            log.info("Atualizando horário de trabalho do tenant {}", tenantId);
            return workingHoursRepository.save(workingHours);
        } else {
            // Cria nova configuração
            // Busca ou cria um profissional para associar ao working hours
            ProfessionalEntity professional = getOrCreateProfessionalForTenant(tenantId);

            TenantWorkingHoursEntity workingHours = new TenantWorkingHoursEntity();
            workingHours.setTenantId(tenantId);
            workingHours.setProfessional(professional);
            workingHours.setStartTime(startTime);
            workingHours.setEndTime(endTime);
            workingHours.setSlotIntervalMinutes(slotIntervalMinutes);
            workingHours.setActive(true);
            log.info("Criando horário de trabalho para tenant {} com profissional {}", tenantId, professional.getId());
            return workingHoursRepository.save(workingHours);
        }
    }

    /**
     * Valida se os horários de trabalho são consistentes.
     *
     * @throws BusinessException se os horários forem inválidos
     */
    private void validateWorkingHours(LocalTime startTime, LocalTime endTime, Integer slotIntervalMinutes) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BusinessException("Horário de início deve ser antes do horário de término");
        }

        if (slotIntervalMinutes == null || slotIntervalMinutes <= 0 || slotIntervalMinutes > 120) {
            throw new BusinessException("Intervalo de slots deve ser entre 1 e 120 minutos");
        }
    }

    /**
     * Remove a configuração de horário de trabalho de um tenant.
     * O tenant voltará a usar os horários padrão.
     */
    @Transactional
    public void deleteWorkingHours() {
        String tenantId = TenantContext.getTenantId();
        Optional<TenantWorkingHoursEntity> existing = workingHoursRepository.findByTenantId(tenantId);

        if (existing.isPresent()) {
            workingHoursRepository.delete(existing.get());
            log.info("Removendo configuração de horário do tenant {}", tenantId);
        }
    }

    /**
     * Verifica se um horário está dentro do expediente do tenant.
     *
     * @param time     Horário a verificar
     * @param tenantId ID do tenant
     * @return true se está dentro do expediente
     */
    public boolean isWithinWorkingHours(LocalTime time, String tenantId) {
        TenantWorkingHoursEntity workingHours = getWorkingHours(tenantId);
        return !time.isBefore(workingHours.getStartTime()) && !time.isAfter(workingHours.getEndTime());
    }

    /**
     * Verifica se um intervalo de tempo está totalmente dentro do expediente do tenant.
     *
     * @param startTime Horário de início
     * @param endTime   Horário de término
     * @param tenantId  ID do tenant
     * @return true se o intervalo está dentro do expediente
     */
    public boolean isIntervalWithinWorkingHours(LocalTime startTime, LocalTime endTime, String tenantId) {
        TenantWorkingHoursEntity workingHours = getWorkingHours(tenantId);
        return !startTime.isBefore(workingHours.getStartTime()) && !endTime.isAfter(workingHours.getEndTime());
    }
}

