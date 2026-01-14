package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.TenantWorkingHoursRequest;
import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import lash_salao_kc.agendamento_back.service.TenantWorkingHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de horários de trabalho dos profissionais.
 * Permite configurar horários personalizados por tenant.
 */
@RestController
@RequestMapping("/working-hours")
@RequiredArgsConstructor
public class TenantWorkingHoursController extends BaseController {

    private final TenantWorkingHoursService workingHoursService;

    /**
     * Retorna o horário de trabalho configurado para o tenant atual.
     *
     * @return Horário de trabalho (200 OK)
     */
    @GetMapping
    public ResponseEntity<TenantWorkingHoursEntity> getWorkingHours() {
        TenantWorkingHoursEntity workingHours = workingHoursService.getCurrentTenantWorkingHours();
        return ResponseEntity.ok(workingHours);
    }

    /**
     * Configura ou atualiza o horário de trabalho do tenant atual.
     *
     * @param request Dados do horário de trabalho
     * @return Horário de trabalho configurado (200 OK ou 201 Created)
     */
    @PostMapping
    public ResponseEntity<TenantWorkingHoursEntity> configureWorkingHours(
            @Valid @RequestBody TenantWorkingHoursRequest request) {

        TenantWorkingHoursEntity workingHours = workingHoursService.configureWorkingHours(
                request.getStartTime(),
                request.getEndTime(),
                request.getSlotIntervalMinutes()
        );

        return ResponseEntity.ok(workingHours);
    }

    /**
     * Remove a configuração de horário de trabalho do tenant atual.
     * O tenant voltará a usar os horários padrão.
     *
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteWorkingHours() {
        workingHoursService.deleteWorkingHours();
        return ResponseEntity.noContent().build();
    }
}

