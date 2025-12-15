package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

}
