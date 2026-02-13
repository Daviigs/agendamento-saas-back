package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateTenantRequest;
import lash_salao_kc.agendamento_back.domain.dto.UpdateTenantRequest;
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
 *
 * Suporta a palavra-chave "current" no lugar do UUID para operar no tenant do contexto (X-Tenant-Id).
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
     * Busca um tenant específico por ID ou "current" para o tenant do contexto.
     *
     * @param tenantId ID do tenant (UUID) ou "current"
     * @return Tenant encontrado (200 OK)
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantEntity> getTenantById(@PathVariable String tenantId) {
        TenantEntity tenant;
        if ("current".equalsIgnoreCase(tenantId)) {
            tenant = tenantService.getCurrentTenant();
        } else {
            tenant = tenantService.getTenantById(UUID.fromString(tenantId));
        }
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
     * Aceita UUID do tenant ou "current" para atualizar o tenant do contexto (header X-Tenant-Id).
     *
     * @param tenantId ID do tenant (UUID) ou "current"
     * @param request  Dados atualizados (não requer tenantKey)
     * @return Tenant atualizado (200 OK)
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantEntity> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantEntity tenant;
        if ("current".equalsIgnoreCase(tenantId)) {
            TenantEntity currentTenant = tenantService.getCurrentTenant();
            tenant = tenantService.updateTenant(currentTenant.getId(), request);
        } else {
            tenant = tenantService.updateTenant(UUID.fromString(tenantId), request);
        }
        return ResponseEntity.ok(tenant);
    }

    /**
     * Ativa um tenant.
     * Aceita UUID do tenant ou "current" para ativar o tenant do contexto.
     *
     * @param tenantId ID do tenant (UUID) ou "current"
     * @return Tenant ativado (200 OK)
     */
    @PatchMapping("/{tenantId}/activate")
    public ResponseEntity<TenantEntity> activateTenant(@PathVariable String tenantId) {
        TenantEntity tenant;
        if ("current".equalsIgnoreCase(tenantId)) {
            TenantEntity currentTenant = tenantService.getCurrentTenant();
            tenant = tenantService.setTenantActive(currentTenant.getId(), true);
        } else {
            tenant = tenantService.setTenantActive(UUID.fromString(tenantId), true);
        }
        return ResponseEntity.ok(tenant);
    }

    /**
     * Desativa um tenant.
     * Aceita UUID do tenant ou "current" para desativar o tenant do contexto.
     *
     * @param tenantId ID do tenant (UUID) ou "current"
     * @return Tenant desativado (200 OK)
     */
    @PatchMapping("/{tenantId}/deactivate")
    public ResponseEntity<TenantEntity> deactivateTenant(@PathVariable String tenantId) {
        TenantEntity tenant;
        if ("current".equalsIgnoreCase(tenantId)) {
            TenantEntity currentTenant = tenantService.getCurrentTenant();
            tenant = tenantService.setTenantActive(currentTenant.getId(), false);
        } else {
            tenant = tenantService.setTenantActive(UUID.fromString(tenantId), false);
        }
        return ResponseEntity.ok(tenant);
    }
}

