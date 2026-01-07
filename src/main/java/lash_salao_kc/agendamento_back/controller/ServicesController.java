package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.config.TenantContext;
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

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServicesController {
    private final ServicesService servicesService;

    /**
     * POST /services
     * Cria um novo serviço
     */
    @PostMapping
    public ResponseEntity<ServicesEntity> createService(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateServiceRequest request) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        ServicesEntity entity = new ServicesEntity();
        entity.setTenantId(tenantId);
        entity.setName(request.getName());
        entity.setDuration(request.getDuration());
        entity.setPrice(request.getPrice());
        ServicesEntity saved = servicesService.saveService(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /services
     * Lista todos os serviços
     */
    @GetMapping
    public ResponseEntity<List<ServicesEntity>> getAllServices(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        List<ServicesEntity> services = servicesService.findAll();
        return ResponseEntity.ok(services);
    }

    /**
     * GET /services/{id}
     * Busca um serviço por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicesEntity> getServiceById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        ServicesEntity service = servicesService.findById(id);
        return ResponseEntity.ok(service);
    }

    /**
     * PUT /services/{id}
     * Atualiza um serviço existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServicesEntity> updateService(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        ServicesEntity updatedService = new ServicesEntity();
        updatedService.setName(request.getName());
        updatedService.setDuration(request.getDuration());
        updatedService.setPrice(request.getPrice());
        ServicesEntity updated = servicesService.updateService(id, updatedService);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /services/{id}
     * Deleta um serviço
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        servicesService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
