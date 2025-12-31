package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;

    public ServicesEntity saveService(ServicesEntity entity){
        String tenantId = TenantContext.getTenantId();
        entity.setTenantId(tenantId);
        return servicesRepository.save(entity);
    }

    public List<ServicesEntity> findAll(){
        String tenantId = TenantContext.getTenantId();
        return servicesRepository.findByTenantId(tenantId);
    }

    /**
     * Busca um serviço por ID (filtrado por tenant)
     */
    public ServicesEntity findById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        return servicesRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + id));
    }

    /**
     * Atualiza um serviço existente
     */
    @Transactional
    public ServicesEntity updateService(UUID id, ServicesEntity updatedService) {
        ServicesEntity existing = findById(id);

        // Atualiza os campos
        existing.setName(updatedService.getName());
        existing.setDuration(updatedService.getDuration());
        existing.setPrice(updatedService.getPrice());

        return servicesRepository.save(existing);
    }

    /**
     * Deleta um serviço por ID
     */
    @Transactional
    public void deleteService(UUID id) {
        ServicesEntity service = findById(id);
        servicesRepository.delete(service);
    }

}
