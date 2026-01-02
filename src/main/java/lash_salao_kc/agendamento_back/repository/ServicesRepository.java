package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicesRepository extends JpaRepository<ServicesEntity, UUID> {

    /**
     * Busca todos os serviços de um tenant específico
     */
    List<ServicesEntity> findByTenantId(String tenantId);

    /**
     * Busca um serviço por ID e tenant
     */
    Optional<ServicesEntity> findByIdAndTenantId(UUID id, String tenantId);
}
