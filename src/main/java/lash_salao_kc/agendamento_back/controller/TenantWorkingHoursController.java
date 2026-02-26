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
 * Controller REST para gerenciamento de horários de funcionamento do tenant.
 * O horário é único por tenant — todos os profissionais compartilham o mesmo expediente.
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
                request.getSlotIntervalMinutes(),
                request.getHorarioFlexivel()
        );

        return ResponseEntity.ok(workingHours);
    }

    /**
     * Atualiza apenas a flag de horário flexível do tenant atual.
     *
     * @param horarioFlexivel true para modo flexível, false para modo rígido
     * @return Horário de trabalho atualizado (200 OK)
     */
    @PatchMapping("/horario-flexivel")
    public ResponseEntity<TenantWorkingHoursEntity> updateHorarioFlexivel(
            @RequestParam("flexivel") Boolean horarioFlexivel) {

        TenantWorkingHoursEntity workingHours = workingHoursService.updateHorarioFlexivel(horarioFlexivel);
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

