package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.BlockRecurringTimeSlotRequest;
import lash_salao_kc.agendamento_back.domain.dto.BlockSpecificTimeSlotRequest;
import lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity;
import lash_salao_kc.agendamento_back.service.BlockedTimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de bloqueios de horários específicos.
 * Permite bloquear intervalos de tempo em datas específicas ou de forma recorrente.
 */
@RestController
@RequestMapping("/blocked-time-slots")
@RequiredArgsConstructor
public class BlockedTimeSlotController extends BaseController {

    private final BlockedTimeSlotService blockedTimeSlotService;

    /**
     * Bloqueia um intervalo de horário em uma data específica.
     *
     * @param request Dados do bloqueio
     * @return Bloqueio criado (201 Created)
     */
    @PostMapping("/specific")
    public ResponseEntity<BlockedTimeSlotEntity> blockSpecificTimeSlot(
            @Valid @RequestBody BlockSpecificTimeSlotRequest request) {

        BlockedTimeSlotEntity blockedSlot = blockedTimeSlotService.blockSpecificTimeSlot(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getReason()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(blockedSlot);
    }

    /**
     * Bloqueia um intervalo de horário de forma recorrente em um dia da semana.
     *
     * @param request Dados do bloqueio recorrente
     * @return Bloqueio criado (201 Created)
     */
    @PostMapping("/recurring")
    public ResponseEntity<BlockedTimeSlotEntity> blockRecurringTimeSlot(
            @Valid @RequestBody BlockRecurringTimeSlotRequest request) {

        BlockedTimeSlotEntity blockedSlot = blockedTimeSlotService.blockRecurringTimeSlot(
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                request.getReason()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(blockedSlot);
    }

    /**
     * Remove um bloqueio de horário existente (desbloqueia).
     *
     * @param blockedSlotId ID do bloqueio a ser removido
     * @return 204 No Content
     */
    @DeleteMapping("/{blockedSlotId}")
    public ResponseEntity<Void> unblockTimeSlot(@PathVariable UUID blockedSlotId) {
        blockedTimeSlotService.unblockTimeSlot(blockedSlotId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna todos os bloqueios de horário do tenant atual.
     *
     * @return Lista de bloqueios (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<BlockedTimeSlotEntity>> getAllBlockedTimeSlots() {
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getAllBlockedTimeSlots();
        return ResponseEntity.ok(blockedSlots);
    }

    /**
     * Retorna bloqueios de horário para uma data específica.
     *
     * @param date Data a consultar
     * @return Lista de bloqueios ativos na data (200 OK)
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<BlockedTimeSlotEntity>> getBlockedTimeSlotsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getBlockedTimeSlotsForDate(date);
        return ResponseEntity.ok(blockedSlots);
    }

    /**
     * Retorna apenas bloqueios recorrentes do tenant atual.
     *
     * @return Lista de bloqueios recorrentes (200 OK)
     */
    @GetMapping("/recurring")
    public ResponseEntity<List<BlockedTimeSlotEntity>> getRecurringBlockedTimeSlots() {
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getRecurringBlockedTimeSlots();
        return ResponseEntity.ok(blockedSlots);
    }

    /**
     * Retorna apenas bloqueios de datas específicas do tenant atual.
     *
     * @return Lista de bloqueios específicos (200 OK)
     */
    @GetMapping("/specific")
    public ResponseEntity<List<BlockedTimeSlotEntity>> getSpecificBlockedTimeSlots() {
        List<BlockedTimeSlotEntity> blockedSlots = blockedTimeSlotService.getSpecificBlockedTimeSlots();
        return ResponseEntity.ok(blockedSlots);
    }
}

