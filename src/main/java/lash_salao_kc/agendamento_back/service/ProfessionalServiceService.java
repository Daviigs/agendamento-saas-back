package lash_salao_kc.agendamento_back.service;

import jakarta.persistence.EntityManager;
import lash_salao_kc.agendamento_back.domain.dto.ProfessionalServicesResponse;
import lash_salao_kc.agendamento_back.domain.dto.ServiceSummary;
import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import lash_salao_kc.agendamento_back.domain.entity.ProfessionalServiceEntity;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.ProfessionalRepository;
import lash_salao_kc.agendamento_back.repository.ProfessionalServiceRepository;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de vínculos entre profissionais e serviços.
 * Implementa a regra de negócio onde profissionais executam serviços do tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalServiceService {

    private final ProfessionalServiceRepository professionalServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServicesRepository servicesRepository;
    private final EntityManager entityManager;

    /**
     * Vincula uma lista de serviços a um profissional.
     * Remove vínculos antigos e cria novos.
     * Valida que todos os serviços pertencem ao mesmo tenant do profissional.
     *
     * @param professionalId ID do profissional
     * @param serviceIds     Lista de IDs dos serviços
     * @param tenantId       ID do tenant
     * @return Resposta com serviços vinculados
     * @throws BusinessException se houver serviços de outro tenant
     */
    @Transactional
    public ProfessionalServicesResponse linkServicesToProfessional(
            UUID professionalId,
            List<UUID> serviceIds,
            UUID tenantId) {

        // Valida profissional
        ProfessionalEntity professional = professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado ou não pertence ao tenant"));

        // Valida serviços
        List<ServicesEntity> services = validateAndFetchServices(serviceIds, professional.getTenant().getTenantKey());

        // Remove vínculos antigos
        professionalServiceRepository.deleteByProfessionalId(professionalId);
        entityManager.flush(); // Força a execução do DELETE antes de continuar
        log.info("Vínculos antigos removidos para profissional: {}", professionalId);

        // Cria novos vínculos
        List<ProfessionalServiceEntity> newLinks = new ArrayList<>();
        for (ServicesEntity service : services) {
            ProfessionalServiceEntity link = new ProfessionalServiceEntity();
            link.setProfessional(professional);
            link.setService(service);
            newLinks.add(link);
        }

        professionalServiceRepository.saveAll(newLinks);
        log.info("Vinculados {} serviços ao profissional {}", services.size(), professional.getProfessionalName());

        return buildResponse(professional, services);
    }

    /**
     * Remove um vínculo específico entre profissional e serviço.
     *
     * @param professionalId ID do profissional
     * @param serviceId      ID do serviço
     * @param tenantId       ID do tenant
     */
    @Transactional
    public void unlinkServiceFromProfessional(UUID professionalId, UUID serviceId, UUID tenantId) {

        // Valida profissional pertence ao tenant
        ProfessionalEntity professional = professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado ou não pertence ao tenant"));

        // Busca e remove o vínculo
        ProfessionalServiceEntity link = professionalServiceRepository
                .findByProfessionalIdAndServiceId(professionalId, serviceId)
                .orElseThrow(() -> new BusinessException("Vínculo não encontrado"));

        professionalServiceRepository.delete(link);
        log.info("Serviço {} desvinculado do profissional {}", serviceId, professional.getProfessionalName());
    }

    /**
     * Lista todos os serviços vinculados a um profissional.
     *
     * @param professionalId ID do profissional
     * @param tenantId       ID do tenant
     * @return Resposta com serviços vinculados
     */
    public ProfessionalServicesResponse getServicesByProfessional(UUID professionalId, UUID tenantId) {

        // Valida profissional
        ProfessionalEntity professional = professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado ou não pertence ao tenant"));

        // Busca vínculos
        List<ProfessionalServiceEntity> links = professionalServiceRepository.findByProfessionalId(professionalId);

        List<ServicesEntity> services = links.stream()
                .map(ProfessionalServiceEntity::getService)
                .collect(Collectors.toList());

        return buildResponse(professional, services);
    }

    /**
     * Valida se um profissional executa TODOS os serviços da lista.
     * Usado durante a criação de agendamento.
     *
     * @param professionalId ID do profissional
     * @param serviceIds     Lista de IDs dos serviços
     * @return true se o profissional executa todos os serviços
     */
    public boolean professionalExecutesAllServices(UUID professionalId, List<UUID> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return false;
        }

        return professionalServiceRepository.professionalExecutesAllServices(
                professionalId,
                serviceIds,
                serviceIds.size()
        );
    }

    /**
     * Busca profissionais que executam TODOS os serviços da lista.
     * Usado para filtrar profissionais disponíveis na interface.
     *
     * @param serviceIds Lista de IDs dos serviços
     * @param tenantId   ID do tenant
     * @return Lista de IDs de profissionais qualificados
     */
    public List<UUID> getProfessionalsByServices(List<UUID> serviceIds, UUID tenantId) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            // Se nenhum serviço foi selecionado, retorna todos os profissionais ativos
            return professionalRepository.findActiveByTenantId(tenantId).stream()
                    .map(ProfessionalEntity::getId)
                    .collect(Collectors.toList());
        }

        return professionalServiceRepository.findProfessionalIdsByAllServices(
                serviceIds,
                tenantId,
                serviceIds.size()
        );
    }

    /**
     * Valida e busca serviços, garantindo que pertencem ao tenant.
     */
    private List<ServicesEntity> validateAndFetchServices(List<UUID> serviceIds, String tenantKey) {
        List<ServicesEntity> services = new ArrayList<>();

        for (UUID serviceId : serviceIds) {
            ServicesEntity service = servicesRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço", serviceId));

            // Valida que o serviço pertence ao tenant
            if (!service.getTenantId().equals(tenantKey)) {
                throw new BusinessException(
                        String.format("Serviço %s não pertence ao tenant %s", serviceId, tenantKey));
            }

            services.add(service);
        }

        return services;
    }

    /**
     * Constrói resposta com dados do profissional e serviços.
     */
    private ProfessionalServicesResponse buildResponse(ProfessionalEntity professional, List<ServicesEntity> services) {
        List<ServiceSummary> serviceSummaries = services.stream()
                .map(this::toServiceSummary)
                .collect(Collectors.toList());

        return new ProfessionalServicesResponse(
                professional.getId(),
                professional.getProfessionalName(),
                serviceSummaries
        );
    }

    /**
     * Converte entidade de serviço para DTO resumido.
     */
    private ServiceSummary toServiceSummary(ServicesEntity service) {
        return new ServiceSummary(
                service.getId(),
                service.getName(),
                service.getDuration(),
                service.getPrice()
        );
    }
}

