package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateProfessionalRequest;
import lash_salao_kc.agendamento_back.domain.dto.ProfessionalResponse;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.service.ProfessionalService;
import lash_salao_kc.agendamento_back.service.ProfessionalServiceService;
import lash_salao_kc.agendamento_back.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de profissionais.
 * Permite listar, criar e gerenciar profissionais de um tenant.
 */
@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalController extends BaseController {

    private final ProfessionalService professionalService;
    private final TenantService tenantService;
    private final ProfessionalServiceService professionalServiceService;

    /**
     * Lista todos os profissionais do tenant atual.
     *
     * @return Lista de profissionais (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> getProfessionals() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        List<ProfessionalResponse> professionals = professionalService.getProfessionalsByTenant(tenant.getId());
        return ResponseEntity.ok(professionals);
    }

    /**
     * Lista apenas profissionais ativos do tenant atual.
     *
     * @return Lista de profissionais ativos (200 OK)
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProfessionalResponse>> getActiveProfessionals(
            @RequestParam(required = false) List<UUID> serviceIds) {

        TenantEntity tenant = tenantService.getCurrentTenant();

        // Se serviços foram especificados, filtra profissionais que executam TODOS os serviços
        if (serviceIds != null && !serviceIds.isEmpty()) {
            List<UUID> qualifiedProfessionalIds = professionalServiceService
                    .getProfessionalsByServices(serviceIds, tenant.getId());

            // Busca dados completos dos profissionais qualificados
            List<ProfessionalResponse> professionals = professionalService
                    .getActiveProfessionalsByTenant(tenant.getId())
                    .stream()
                    .filter(p -> qualifiedProfessionalIds.contains(p.getId()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(professionals);
        }

        // Comportamento original: retorna todos os profissionais ativos
        List<ProfessionalResponse> professionals = professionalService
                .getActiveProfessionalsByTenant(tenant.getId());
        return ResponseEntity.ok(professionals);
    }

    /**
     * Busca um profissional específico por ID.
     *
     * @param professionalId ID do profissional
     * @return Profissional encontrado (200 OK)
     */
    @GetMapping("/{professionalId}")
    public ResponseEntity<ProfessionalResponse> getProfessionalById(@PathVariable UUID professionalId) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        var professional = professionalService.getProfessionalByIdAndTenant(professionalId, tenant.getId());

        ProfessionalResponse response = new ProfessionalResponse();
        response.setId(professional.getId());
        response.setTenantId(professional.getTenantId());
        response.setProfessionalName(professional.getProfessionalName());
        response.setProfessionalEmail(professional.getProfessionalEmail());
        response.setProfessionalPhone(professional.getProfessionalPhone());
        response.setActive(professional.getActive());

        return ResponseEntity.ok(response);
    }

    /**
     * Cria um novo profissional para o tenant atual.
     *
     * @param request Dados do profissional
     * @return Profissional criado (201 Created)
     */
    @PostMapping
    public ResponseEntity<ProfessionalResponse> createProfessional(@Valid @RequestBody CreateProfessionalRequest request) {
        ProfessionalResponse professional = professionalService.createProfessional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(professional);
    }

    /**
     * Atualiza um profissional existente.
     *
     * @param professionalId ID do profissional
     * @param request        Dados atualizados
     * @return Profissional atualizado (200 OK)
     */
    @PutMapping("/{professionalId}")
    public ResponseEntity<ProfessionalResponse> updateProfessional(
            @PathVariable UUID professionalId,
            @Valid @RequestBody CreateProfessionalRequest request) {
        ProfessionalResponse professional = professionalService.updateProfessional(professionalId, request);
        return ResponseEntity.ok(professional);
    }

    /**
     * Ativa um profissional.
     *
     * @param professionalId ID do profissional
     * @return Profissional ativado (200 OK)
     */
    @PatchMapping("/{professionalId}/activate")
    public ResponseEntity<ProfessionalResponse> activateProfessional(@PathVariable UUID professionalId) {
        ProfessionalResponse professional = professionalService.setProfessionalActive(professionalId, true);
        return ResponseEntity.ok(professional);
    }

    /**
     * Desativa um profissional.
     *
     * @param professionalId ID do profissional
     * @return Profissional desativado (200 OK)
     */
    @PatchMapping("/{professionalId}/deactivate")
    public ResponseEntity<ProfessionalResponse> deactivateProfessional(@PathVariable UUID professionalId) {
        ProfessionalResponse professional = professionalService.setProfessionalActive(professionalId, false);
        return ResponseEntity.ok(professional);
    }
}

