package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateTenantRequest;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de tenants (salões/clientes).
 * Endpoints administrativos para criar e gerenciar tenants do sistema.
 */
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * Lista todos os tenants.
     *
     * @return Lista de tenants (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<TenantEntity>> getAllTenants() {
        List<TenantEntity> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Busca um tenant específico por ID.
     *
     * @param tenantId ID do tenant
     * @return Tenant encontrado (200 OK)
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantEntity> getTenantById(@PathVariable UUID tenantId) {
        TenantEntity tenant = tenantService.getTenantById(tenantId);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Cria um novo tenant.
     *
     * @param request Dados do tenant
     * @return Tenant criado (201 Created)
     */
    @PostMapping
    public ResponseEntity<TenantEntity> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantEntity tenant = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /**
     * Atualiza um tenant existente.
     *
     * @param tenantId ID do tenant
     * @param request  Dados atualizados
     * @return Tenant atualizado (200 OK)
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantEntity> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateTenantRequest request) {
        TenantEntity tenant = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Ativa um tenant.
     *
     * @param tenantId ID do tenant
     * @return Tenant ativado (200 OK)
     */
    @PatchMapping("/{tenantId}/activate")
    public ResponseEntity<TenantEntity> activateTenant(@PathVariable UUID tenantId) {
        TenantEntity tenant = tenantService.setTenantActive(tenantId, true);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Desativa um tenant.
     *
     * @param tenantId ID do tenant
     * @return Tenant desativado (200 OK)
     */
    @PatchMapping("/{tenantId}/deactivate")
    public ResponseEntity<TenantEntity> deactivateTenant(@PathVariable UUID tenantId) {
        TenantEntity tenant = tenantService.setTenantActive(tenantId, false);
        return ResponseEntity.ok(tenant);
    }
}

