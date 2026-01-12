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
import java.util.UUID;

/**
 * Controller REST para gerenciamento de dias bloqueados.
 * Permite bloquear datas específicas ou dias da semana recorrentes.
 *
 * NOTA: Não é necessário receber X-Tenant-Id nos métodos pois o TenantInterceptor
 * já valida e injeta o tenant no contexto antes dos métodos serem chamados.
 */
@RestController
@RequestMapping("/blocked-days")
@RequiredArgsConstructor
public class BlockedDayController extends BaseController {

    private final BlockedDayService blockedDayService;

    /**
     * Bloqueia uma data específica para agendamentos.
     *
     * @param request Dados do bloqueio (data e motivo)
     * @return Bloqueio criado (201 Created)
     */
    @PostMapping("/specific")
    public ResponseEntity<BlockedDayEntity> blockSpecificDate(@Valid @RequestBody BlockSpecificDateRequest request) {
        BlockedDayEntity blockedDay = blockedDayService.blockSpecificDate(
                request.getDate(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * Bloqueia um dia da semana de forma recorrente.
     *
     * @param request Dados do bloqueio (dia da semana e motivo)
     * @return Bloqueio criado (201 Created)
     */
    @PostMapping("/recurring")
    public ResponseEntity<BlockedDayEntity> blockRecurringDay(@Valid @RequestBody BlockRecurringDayRequest request) {
        BlockedDayEntity blockedDay = blockedDayService.blockRecurringDayOfWeek(
                request.getDayOfWeek(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedDay);
    }

    /**
     * Retorna todos os bloqueios (específicos e recorrentes).
     *
     * @return Lista de bloqueios (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<BlockedDayEntity>> getAllBlockedDays() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getAllBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * Retorna apenas bloqueios de datas específicas.
     *
     * @return Lista de bloqueios específicos (200 OK)
     */
    @GetMapping("/specific")
    public ResponseEntity<List<BlockedDayEntity>> getSpecificBlockedDates() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getSpecificBlockedDates();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * Retorna apenas bloqueios recorrentes (dias da semana).
     *
     * @return Lista de bloqueios recorrentes (200 OK)
     */
    @GetMapping("/recurring")
    public ResponseEntity<List<BlockedDayEntity>> getRecurringBlockedDays() {
        List<BlockedDayEntity> blockedDays = blockedDayService.getRecurringBlockedDays();
        return ResponseEntity.ok(blockedDays);
    }

    /**
     * Retorna datas disponíveis (não bloqueadas) em um período.
     *
     * @param startDate Data inicial do período
     * @param endDate   Data final do período
     * @return Lista de datas disponíveis (200 OK)
     */
    @GetMapping("/available")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LocalDate> availableDates = blockedDayService.getAvailableDates(startDate, endDate);
        return ResponseEntity.ok(availableDates);
    }

    /**
     * Remove um bloqueio existente.
     *
     * @param blockedDayId ID do bloqueio a remover
     * @return Resposta vazia (204 No Content)
     */
    @DeleteMapping("/{blockedDayId}")
    public ResponseEntity<Void> unblockDay(@PathVariable UUID blockedDayId) {
        blockedDayService.unblockDay(blockedDayId);
        return ResponseEntity.noContent().build();
    }
}

