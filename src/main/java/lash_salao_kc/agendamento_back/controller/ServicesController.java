package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.dto.*;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.service.ServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServicesController {
    private final ServicesService servicesService;

    /**
     * POST /services/create
     * Cria um novo serviço
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "name": "Extensão de Cílios",
     *   "duration": 90,
     *   "price": 150.00
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<ServicesEntity> createService(@Valid @RequestBody CreateServiceRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        ServicesEntity entity = new ServicesEntity();
        entity.setTenantId(request.getTenantId());
        entity.setName(request.getName());
        entity.setDuration(request.getDuration());
        entity.setPrice(request.getPrice());
        ServicesEntity saved = servicesService.saveService(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /services/list
     * Lista todos os serviços
     *
     * Request body:
     * {
     *   "tenantId": "cliente1"
     * }
     */
    @PostMapping("/list")
    public ResponseEntity<List<ServicesEntity>> getAllServices(@Valid @RequestBody TenantRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<ServicesEntity> services = servicesService.findAll();
        return ResponseEntity.ok(services);
    }

    /**
     * POST /services/by-id
     * Busca um serviço por ID
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "id": "uuid-do-servico"
     * }
     */
    @PostMapping("/by-id")
    public ResponseEntity<ServicesEntity> getServiceById(@Valid @RequestBody TenantIdWithId request) {
        TenantContext.setTenantId(request.getTenantId());
        ServicesEntity service = servicesService.findById(UUID.fromString(request.getId()));
        return ResponseEntity.ok(service);
    }

    /**
     * POST /services/update
     * Atualiza um serviço existente
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "id": "uuid-do-servico",
     *   "name": "Design de Sobrancelhas",
     *   "duration": 60,
     *   "price": 80.00
     * }
     */
    @PostMapping("/update")
    public ResponseEntity<ServicesEntity> updateService(@Valid @RequestBody UpdateServiceWithIdRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        ServicesEntity updatedService = new ServicesEntity();
        updatedService.setName(request.getName());
        updatedService.setDuration(request.getDuration());
        updatedService.setPrice(request.getPrice());
        ServicesEntity updated = servicesService.updateService(UUID.fromString(request.getId()), updatedService);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /services/delete
     * Deleta um serviço
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "id": "uuid-do-servico"
     * }
     */
    @PostMapping("/delete")
    public ResponseEntity<Void> deleteService(@Valid @RequestBody TenantIdWithId request) {
        TenantContext.setTenantId(request.getTenantId());
        servicesService.deleteService(UUID.fromString(request.getId()));
        return ResponseEntity.noContent().build();
    }
}
