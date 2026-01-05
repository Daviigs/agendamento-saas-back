package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.dto.*;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayEntity;
import lash_salao_kc.agendamento_back.service.BlockedDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/blocked-days")
@RequiredArgsConstructor
public class BlockedDayController {

    private final BlockedDayService blockedDayService;

    /**
     * POST /blocked-days/specific
     * Bloqueia uma data específica (ex: feriado, evento especial)
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "date": "2025-12-25",
     *   "reason": "Natal"
     * }
     */
    @PostMapping("/specific")
    public ResponseEntity<BlockedDayEntity> blockSpecificDate(@Valid @RequestBody BlockSpecificDateRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        BlockedDayEntity blockedDay = blockedDayService.blockSpecificDate(request.getDate(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * POST /blocked-days/recurring
     * Bloqueia um dia da semana recorrente (ex: todo domingo)
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "dayOfWeek": "SUNDAY",
     *   "reason": "Folga semanal"
     * }
     *
     * Dias da semana: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     */
    @PostMapping("/recurring")
    public ResponseEntity<BlockedDayEntity> blockRecurringDay(@Valid @RequestBody BlockRecurringDayRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        BlockedDayEntity blockedDay = blockedDayService.blockRecurringDayOfWeek(request.getDayOfWeek(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * POST /blocked-days/list
     * Lista todos os dias bloqueados (específicos e recorrentes)
     *
     * Request body:
     * {
     *   "tenantId": "cliente1"
     * }
     */
    @PostMapping("/list")
    public ResponseEntity<List<BlockedDayEntity>> getAllBlockedDays(@Valid @RequestBody TenantRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<BlockedDayEntity> blockedDays = blockedDayService.getAllBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * POST /blocked-days/specific/list
     * Lista apenas bloqueios de datas específicas
     *
     * Request body:
     * {
     *   "tenantId": "cliente1"
     * }
     */
    @PostMapping("/specific/list")
    public ResponseEntity<List<BlockedDayEntity>> getSpecificBlockedDates(@Valid @RequestBody TenantRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<BlockedDayEntity> blockedDays = blockedDayService.getSpecificBlockedDates();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * POST /blocked-days/recurring/list
     * Lista apenas bloqueios recorrentes (dias da semana)
     *
     * Request body:
     * {
     *   "tenantId": "cliente1"
     * }
     */
    @PostMapping("/recurring/list")
    public ResponseEntity<List<BlockedDayEntity>> getRecurringBlockedDays(@Valid @RequestBody TenantRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<BlockedDayEntity> blockedDays = blockedDayService.getRecurringBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * POST /blocked-days/available
     * Retorna lista de datas disponíveis (não bloqueadas) dentro de um período
     *
     * Útil para exibir calendário com dias disponíveis
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "startDate": "2025-12-27",
     *   "endDate": "2026-01-31"
     * }
     *
     * @param request Dados da requisição
     * @return Lista de datas disponíveis
     *
     * Exemplo: POST /blocked-days/available
     * Body: {"tenantId": "cliente1", "startDate": "2025-12-27", "endDate": "2025-12-31"}
     * Resposta: ["2025-12-27", "2025-12-29", "2025-12-30", "2025-12-31"]
     * (assumindo que 2025-12-28 é domingo e está bloqueado)
     */
    @PostMapping("/available")
    public ResponseEntity<List<LocalDate>> getAvailableDates(@Valid @RequestBody GetAvailableDatesRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<LocalDate> availableDates = blockedDayService.getAvailableDates(request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(availableDates);
    }

    /**
     * POST /blocked-days/unblock
     * Remove um bloqueio (libera o dia)
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "id": "uuid-do-bloqueio"
     * }
     */
    @PostMapping("/unblock")
    public ResponseEntity<Void> unblockDay(@Valid @RequestBody TenantIdWithId request) {
        TenantContext.setTenantId(request.getTenantId());
        blockedDayService.unblockDay(java.util.UUID.fromString(request.getId()));
        return ResponseEntity.noContent().build();
    }
}

