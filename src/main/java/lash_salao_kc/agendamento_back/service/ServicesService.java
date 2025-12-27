package lash_salao_kc.agendamento_back.service;

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
        return servicesRepository.save(entity);
    }

    public List<ServicesEntity> findAll(){
        return servicesRepository.findAll();
    }

    /**
     * Busca um serviço por ID
     */
    public ServicesEntity findById(UUID id) {
        return servicesRepository.findById(id)
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
