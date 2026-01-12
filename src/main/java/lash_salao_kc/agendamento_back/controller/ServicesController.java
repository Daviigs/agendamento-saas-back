package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateServiceRequest;
import lash_salao_kc.agendamento_back.domain.dto.UpdateServiceRequest;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.service.ServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de serviços oferecidos.
 * Expõe endpoints para operações CRUD de serviços.
 *
 * NOTA: Não é necessário receber X-Tenant-Id nos métodos pois o TenantInterceptor
 * já valida e injeta o tenant no contexto antes dos métodos serem chamados.
 */
@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServicesController extends BaseController {

    private final ServicesService servicesService;

    /**
     * Cria um novo serviço.
     *
     * @param request Dados do serviço
     * @return Serviço criado (201 Created)
     */
    @PostMapping
    public ResponseEntity<ServicesEntity> createService(@Valid @RequestBody CreateServiceRequest request) {
        String tenantId = getTenantFromContext();
        ServicesEntity service = buildServiceEntity(request, tenantId);
        ServicesEntity savedService = servicesService.saveService(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedService);
    }

    /**
     * Retorna todos os serviços do tenant.
     *
     * @return Lista de serviços (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<ServicesEntity>> getAllServices() {
        List<ServicesEntity> services = servicesService.findAll();
        return ResponseEntity.ok(services);
    }

    /**
     * Retorna um serviço específico por ID.
     *
     * @param id ID do serviço
     * @return Serviço encontrado (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicesEntity> getServiceById(@PathVariable UUID id) {
        ServicesEntity service = servicesService.findById(id);
        return ResponseEntity.ok(service);
    }

    /**
     * Atualiza um serviço existente.
     *
     * @param id      ID do serviço a atualizar
     * @param request Dados atualizados
     * @return Serviço atualizado (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServicesEntity> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {

        ServicesEntity serviceToUpdate = buildServiceEntityFromUpdateRequest(request);
        ServicesEntity updatedService = servicesService.updateService(id, serviceToUpdate);
        return ResponseEntity.ok(updatedService);
    }

    /**
     * Deleta um serviço.
     *
     * @param id ID do serviço a deletar
     * @return Resposta vazia (204 No Content)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        servicesService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Constrói entidade de serviço a partir do DTO de criação.
     */
    private ServicesEntity buildServiceEntity(CreateServiceRequest request, String tenantId) {
        ServicesEntity entity = new ServicesEntity();
        entity.setTenantId(tenantId);
        entity.setName(request.getName());
        entity.setDuration(request.getDuration());
        entity.setPrice(request.getPrice());
        return entity;
    }

    /**
     * Constrói entidade de serviço a partir do DTO de atualização.
     */
    private ServicesEntity buildServiceEntityFromUpdateRequest(UpdateServiceRequest request) {
        ServicesEntity entity = new ServicesEntity();
        entity.setName(request.getName());
        entity.setDuration(request.getDuration());
        entity.setPrice(request.getPrice());
        return entity;
    }
}
