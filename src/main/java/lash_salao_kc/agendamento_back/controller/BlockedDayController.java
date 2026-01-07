package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.dto.BlockRecurringDayRequest;
import lash_salao_kc.agendamento_back.domain.dto.BlockSpecificDateRequest;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayEntity;
import lash_salao_kc.agendamento_back.service.BlockedDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/blocked-days")
@RequiredArgsConstructor
public class BlockedDayController {

    private final BlockedDayService blockedDayService;

    /**
     * POST /blocked-days/specific
     * Bloqueia uma data específica (ex: feriado, evento especial)
     */
    @PostMapping("/specific")
    public ResponseEntity<BlockedDayEntity> blockSpecificDate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody BlockSpecificDateRequest request) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        BlockedDayEntity blockedDay = blockedDayService.blockSpecificDate(request.getDate(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * POST /blocked-days/recurring
     * Bloqueia um dia da semana recorrente (ex: todo domingo)
     */
    @PostMapping("/recurring")
    public ResponseEntity<BlockedDayEntity> blockRecurringDay(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody BlockRecurringDayRequest request) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        BlockedDayEntity blockedDay = blockedDayService.blockRecurringDayOfWeek(request.getDayOfWeek(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * GET /blocked-days
     * Lista todos os dias bloqueados (específicos e recorrentes)
     */
    @GetMapping
    public ResponseEntity<List<BlockedDayEntity>> getAllBlockedDays(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        List<BlockedDayEntity> blockedDays = blockedDayService.getAllBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/specific
     * Lista apenas bloqueios de datas específicas
     */
    @GetMapping("/specific")
    public ResponseEntity<List<BlockedDayEntity>> getSpecificBlockedDates(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        List<BlockedDayEntity> blockedDays = blockedDayService.getSpecificBlockedDates();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/recurring
     * Lista apenas bloqueios recorrentes (dias da semana)
     */
    @GetMapping("/recurring")
    public ResponseEntity<List<BlockedDayEntity>> getRecurringBlockedDays(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        List<BlockedDayEntity> blockedDays = blockedDayService.getRecurringBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/available?startDate=2026-01-01&endDate=2026-01-31
     * Retorna lista de datas disponíveis (não bloqueadas) dentro de um período
     */
    @GetMapping("/available")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        List<LocalDate> availableDates = blockedDayService.getAvailableDates(startDate, endDate);
        return ResponseEntity.ok(availableDates);
    }

    /**
     * DELETE /blocked-days/{blockedDayId}
     * Remove um bloqueio (libera o dia)
     */
    @DeleteMapping("/{blockedDayId}")
    public ResponseEntity<Void> unblockDay(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID blockedDayId) {
        tenantId = tenantId.toLowerCase().trim();
        TenantContext.setTenantId(tenantId);
        blockedDayService.unblockDay(blockedDayId);
        return ResponseEntity.noContent().build();
    }
}

