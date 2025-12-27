package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
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
     *   "date": "2025-12-25",
     *   "reason": "Natal"
     * }
     */
    @PostMapping("/specific")
    public ResponseEntity<BlockedDayEntity> blockSpecificDate(@Valid @RequestBody BlockSpecificDateRequest request) {
        BlockedDayEntity blockedDay = blockedDayService.blockSpecificDate(request.getDate(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * POST /blocked-days/recurring
     * Bloqueia um dia da semana recorrente (ex: todo domingo)
     *
     * Request body:
     * {
     *   "dayOfWeek": "SUNDAY",
     *   "reason": "Folga semanal"
     * }
     *
     * Dias da semana: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     */
    @PostMapping("/recurring")
    public ResponseEntity<BlockedDayEntity> blockRecurringDay(@Valid @RequestBody BlockRecurringDayRequest request) {
        BlockedDayEntity blockedDay = blockedDayService.blockRecurringDayOfWeek(request.getDayOfWeek(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * GET /blocked-days
     * Lista todos os dias bloqueados (específicos e recorrentes)
     */
    @GetMapping
    public ResponseEntity<List<BlockedDayEntity>> getAllBlockedDays() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getAllBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/specific
     * Lista apenas bloqueios de datas específicas
     */
    @GetMapping("/specific")
    public ResponseEntity<List<BlockedDayEntity>> getSpecificBlockedDates() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getSpecificBlockedDates();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/recurring
     * Lista apenas bloqueios recorrentes (dias da semana)
     */
    @GetMapping("/recurring")
    public ResponseEntity<List<BlockedDayEntity>> getRecurringBlockedDays() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getRecurringBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * GET /blocked-days/available?startDate=2025-12-27&endDate=2026-01-31
     * Retorna lista de datas disponíveis (não bloqueadas) dentro de um período
     *
     * Útil para exibir calendário com dias disponíveis
     *
     * @param startDate Data inicial do período (formato: YYYY-MM-DD)
     * @param endDate Data final do período (formato: YYYY-MM-DD)
     * @return Lista de datas disponíveis
     *
     * Exemplo: GET /blocked-days/available?startDate=2025-12-27&endDate=2025-12-31
     * Resposta: ["2025-12-27", "2025-12-29", "2025-12-30", "2025-12-31"]
     * (assumindo que 2025-12-28 é domingo e está bloqueado)
     */
    @GetMapping("/available")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<LocalDate> availableDates = blockedDayService.getAvailableDates(startDate, endDate);
        return ResponseEntity.ok(availableDates);
    }

    /**
     * DELETE /blocked-days/{blockedDayId}
     * Remove um bloqueio (libera o dia)
     */
    @DeleteMapping("/{blockedDayId}")
    public ResponseEntity<Void> unblockDay(@PathVariable String blockedDayId) {
        blockedDayService.unblockDay(java.util.UUID.fromString(blockedDayId));
        return ResponseEntity.noContent().build();
    }
}

