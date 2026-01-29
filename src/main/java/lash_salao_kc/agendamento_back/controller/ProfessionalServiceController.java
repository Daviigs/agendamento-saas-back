package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.LinkServicesRequest;
import lash_salao_kc.agendamento_back.domain.dto.ProfessionalServicesResponse;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.service.ProfessionalServiceService;
import lash_salao_kc.agendamento_back.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de vínculos entre profissionais e serviços.
 * Endpoints administrativos para configurar quais serviços cada profissional pode executar.
 */
@Slf4j
@RestController
@RequestMapping("/professionals/{professionalId}/services")
@RequiredArgsConstructor
public class ProfessionalServiceController extends BaseController {

    private final ProfessionalServiceService professionalServiceService;
    private final TenantService tenantService;

    /**
     * Lista todos os serviços vinculados a um profissional.
     *
     * @param professionalId ID do profissional
     * @return Lista de serviços vinculados (200 OK)
     */
    @GetMapping
    public ResponseEntity<ProfessionalServicesResponse> getServicesByProfessional(
            @PathVariable UUID professionalId) {

        TenantEntity tenant = tenantService.getCurrentTenant();
        log.info("Listando serviços do profissional {} do tenant {}", professionalId, tenant.getTenantKey());

        ProfessionalServicesResponse response = professionalServiceService
                .getServicesByProfessional(professionalId, tenant.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Vincula serviços a um profissional.
     * Remove vínculos antigos e cria novos.
     *
     * @param professionalId ID do profissional
     * @param request        Lista de IDs dos serviços
     * @return Serviços vinculados (200 OK)
     */
    @PutMapping
    public ResponseEntity<ProfessionalServicesResponse> linkServicesToProfessional(
            @PathVariable UUID professionalId,
            @Valid @RequestBody LinkServicesRequest request) {

        TenantEntity tenant = tenantService.getCurrentTenant();
        log.info("Vinculando {} serviços ao profissional {} do tenant {}",
                request.getServiceIds().size(), professionalId, tenant.getTenantKey());

        ProfessionalServicesResponse response = professionalServiceService
                .linkServicesToProfessional(professionalId, request.getServiceIds(), tenant.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Remove um vínculo específico entre profissional e serviço.
     *
     * @param professionalId ID do profissional
     * @param serviceId      ID do serviço
     * @return Resposta vazia (204 No Content)
     */
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> unlinkServiceFromProfessional(
            @PathVariable UUID professionalId,
            @PathVariable UUID serviceId) {

        TenantEntity tenant = tenantService.getCurrentTenant();
        log.info("Desvinculando serviço {} do profissional {} do tenant {}",
                serviceId, professionalId, tenant.getTenantKey());

        professionalServiceService.unlinkServiceFromProfessional(professionalId, serviceId, tenant.getId());

        return ResponseEntity.noContent().build();
    }
}

