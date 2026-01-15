package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.dto.CreateProfessionalRequest;
import lash_salao_kc.agendamento_back.domain.dto.ProfessionalResponse;
import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de profissionais.
 * Cada profissional pertence a um único tenant e possui configurações independentes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final TenantService tenantService;

    /**
     * Lista todos os profissionais de um tenant.
     *
     * @param tenantId ID do tenant
     * @return Lista de profissionais
     */
    public List<ProfessionalResponse> getProfessionalsByTenant(UUID tenantId) {
        return professionalRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista profissionais ativos de um tenant.
     *
     * @param tenantId ID do tenant
     * @return Lista de profissionais ativos
     */
    public List<ProfessionalResponse> getActiveProfessionalsByTenant(UUID tenantId) {
        return professionalRepository.findActiveByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca um profissional por ID.
     *
     * @param professionalId ID do profissional
     * @return Profissional encontrado
     * @throws ResourceNotFoundException se não encontrado
     */
    public ProfessionalEntity getProfessionalById(UUID professionalId) {
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", professionalId));
    }

    /**
     * Busca um profissional validando que pertence ao tenant.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return Profissional encontrado
     * @throws BusinessException se o profissional não pertencer ao tenant
     */
    public ProfessionalEntity getProfessionalByIdAndTenant(UUID professionalId, UUID tenantId) {
        return professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new BusinessException(
                        String.format("Profissional %s não pertence ao tenant especificado", professionalId)));
    }

    /**
     * Busca um profissional ativo validando que pertence ao tenant.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return Profissional ativo
     * @throws BusinessException se não encontrado, inativo ou não pertencer ao tenant
     */
    public ProfessionalEntity getActiveProfessionalByIdAndTenant(UUID professionalId, UUID tenantId) {
        return professionalRepository.findActiveByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new BusinessException(
                        String.format("Profissional %s não encontrado, inativo ou não pertence ao tenant", professionalId)));
    }

    /**
     * Cria um novo profissional para o tenant atual.
     *
     * @param request Dados do profissional
     * @return Profissional criado
     */
    @Transactional
    public ProfessionalResponse createProfessional(CreateProfessionalRequest request) {
        TenantEntity tenant = tenantService.getCurrentTenant();

        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setTenant(tenant);
        professional.setProfessionalName(request.getProfessionalName());
        professional.setProfessionalEmail(request.getProfessionalEmail());
        professional.setProfessionalPhone(request.getProfessionalPhone());
        professional.setActive(true);

        ProfessionalEntity saved = professionalRepository.save(professional);
        log.info("Novo profissional criado: {} para tenant: {}",
                saved.getProfessionalName(), tenant.getTenantKey());

        return toResponse(saved);
    }

    /**
     * Atualiza um profissional existente.
     *
     * @param professionalId ID do profissional
     * @param request        Dados atualizados
     * @return Profissional atualizado
     */
    @Transactional
    public ProfessionalResponse updateProfessional(UUID professionalId, CreateProfessionalRequest request) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        ProfessionalEntity professional = getProfessionalByIdAndTenant(professionalId, tenant.getId());

        professional.setProfessionalName(request.getProfessionalName());
        professional.setProfessionalEmail(request.getProfessionalEmail());
        professional.setProfessionalPhone(request.getProfessionalPhone());

        ProfessionalEntity updated = professionalRepository.save(professional);
        log.info("Profissional atualizado: {}", updated.getProfessionalName());

        return toResponse(updated);
    }

    /**
     * Ativa ou desativa um profissional.
     *
     * @param professionalId ID do profissional
     * @param active         true para ativar, false para desativar
     * @return Profissional atualizado
     */
    @Transactional
    public ProfessionalResponse setProfessionalActive(UUID professionalId, boolean active) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        ProfessionalEntity professional = getProfessionalByIdAndTenant(professionalId, tenant.getId());

        professional.setActive(active);
        ProfessionalEntity updated = professionalRepository.save(professional);

        log.info("Profissional {} {}", updated.getProfessionalName(), active ? "ativado" : "desativado");
        return toResponse(updated);
    }

    /**
     * Valida se um profissional pertence ao tenant e está ativo.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @throws BusinessException se validação falhar
     */
    public void validateProfessionalBelongsToTenant(UUID professionalId, UUID tenantId) {
        if (!professionalRepository.existsByIdAndTenantId(professionalId, tenantId)) {
            throw new BusinessException(
                    String.format("Profissional %s não pertence ao tenant especificado", professionalId));
        }
    }

    /**
     * Converte entidade para DTO de resposta.
     */
    private ProfessionalResponse toResponse(ProfessionalEntity entity) {
        ProfessionalResponse response = new ProfessionalResponse();
        response.setId(entity.getId());
        response.setTenantId(entity.getTenantId());
        response.setProfessionalName(entity.getProfessionalName());
        response.setProfessionalEmail(entity.getProfessionalEmail());
        response.setProfessionalPhone(entity.getProfessionalPhone());
        response.setActive(entity.getActive());
        return response;
    }
}

