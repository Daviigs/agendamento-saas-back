package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.repository.ProfessionalServiceRepository;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final ProfessionalServiceRepository professionalServicesRepository;
    private final AppointmentsRepository appointmentsRepository;

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
     * Valida se o serviço não está sendo usado em agendamentos FUTUROS.
     * Permite a exclusão se o serviço só estiver em agendamentos passados.
     * Remove automaticamente as associações com agendamentos passados.
     *
     * @param id ID do serviço a ser deletado
     * @throws ResourceNotFoundException se o serviço não for encontrado
     * @throws BusinessException se o serviço estiver sendo usado em agendamentos futuros
     */
    @Transactional
    public void deleteService(UUID id) {
        ServicesEntity service = findById(id);

        // Valida se o serviço está sendo usado em algum agendamento FUTURO
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (appointmentsRepository.existsFutureAppointmentsByServiceId(id, today, now)) {
            throw new BusinessException(
                    String.format("Não é possível excluir o serviço '%s' pois ele está sendo usado em agendamentos futuros. " +
                            "Remova ou atualize os agendamentos futuros antes de excluir o serviço.", service.getName())
            );
        }

        // Remove vínculos com profissionais
        professionalServicesRepository.deleteByServiceId(id);

        // Remove associações com agendamentos (incluindo agendamentos passados)
        appointmentsRepository.removeServiceFromAppointments(id);

        // Deleta o serviço
        servicesRepository.delete(service);
    }
}


