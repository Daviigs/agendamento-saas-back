package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de serviços oferecidos.
 * Gerencia operações CRUD de serviços com isolamento por tenant.
 */
@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;

    /**
     * Salva um novo serviço no sistema.
     * O tenant ID é automaticamente obtido do contexto da requisição.
     *
     * @param entity Entidade do serviço a ser salva
     * @return Serviço salvo com ID gerado
     */
    public ServicesEntity saveService(ServicesEntity entity) {
        String tenantId = TenantContext.getTenantId();
        entity.setTenantId(tenantId);
        return servicesRepository.save(entity);
    }

    /**
     * Lista todos os serviços do tenant atual.
     *
     * @return Lista de serviços do tenant
     */
    public List<ServicesEntity> findAll() {
        String tenantId = TenantContext.getTenantId();
        return servicesRepository.findByTenantId(tenantId);
    }

    /**
     * Busca um serviço específico por ID.
     * Garante isolamento por tenant.
     *
     * @param id ID do serviço
     * @return Serviço encontrado
     * @throws ResourceNotFoundException se o serviço não for encontrado
     */
    public ServicesEntity findById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        return servicesRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", id));
    }

    /**
     * Atualiza um serviço existente.
     * Apenas nome, duração e preço podem ser atualizados.
     *
     * @param id             ID do serviço a ser atualizado
     * @param updatedService Dados atualizados do serviço
     * @return Serviço atualizado
     * @throws ResourceNotFoundException se o serviço não for encontrado
     */
    @Transactional
    public ServicesEntity updateService(UUID id, ServicesEntity updatedService) {
        ServicesEntity existing = findById(id);

        existing.setName(updatedService.getName());
        existing.setDuration(updatedService.getDuration());
        existing.setPrice(updatedService.getPrice());

        return servicesRepository.save(existing);
    }

    /**
     * Deleta um serviço do sistema.
     *
     * @param id ID do serviço a ser deletado
     * @throws ResourceNotFoundException se o serviço não for encontrado
     */
    @Transactional
    public void deleteService(UUID id) {
        ServicesEntity service = findById(id);
        servicesRepository.delete(service);
    }
}


