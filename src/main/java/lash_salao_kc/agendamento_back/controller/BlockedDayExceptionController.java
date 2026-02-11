package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateBlockedDayExceptionRequest;
import lash_salao_kc.agendamento_back.domain.entity.BlockedDayExceptionEntity;
import lash_salao_kc.agendamento_back.service.BlockedDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de exceções de bloqueios recorrentes.
 * Permite liberar datas específicas que caem em dias bloqueados recorrentes.
 *
 * Exemplo de uso:
 * - Todos os domingos são bloqueados (recorrente)
 * - Mas o dono quer trabalhar no domingo 15/02/2026
 * - Cria-se uma exceção para essa data específica
 *
 * NOTA: Não é necessário receber X-Tenant-Id nos métodos pois o TenantInterceptor
 * já valida e injeta o tenant no contexto antes dos métodos serem chamados.
 */
@RestController
@RequestMapping("/blocked-days/exceptions")
@RequiredArgsConstructor
public class BlockedDayExceptionController extends BaseController {

    private final BlockedDayService blockedDayService;

    /**
     * Cria uma exceção para liberar uma data específica de um bloqueio recorrente.
     *
     * Exemplo: Todos os domingos são bloqueados, mas você quer trabalhar em um domingo específico.
     *
     * @param request Dados da exceção (data e motivo)
     * @return Exceção criada (201 Created)
     */
    @PostMapping
    public ResponseEntity<BlockedDayExceptionEntity> createException(@Valid @RequestBody CreateBlockedDayExceptionRequest request) {
        BlockedDayExceptionEntity exception = blockedDayService.createException(
                request.getExceptionDate(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(exception);
    }

    /**
     * Retorna todas as exceções do tenant.
     *
     * @return Lista de exceções (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<BlockedDayExceptionEntity>> getAllExceptions() {
        List<BlockedDayExceptionEntity> exceptions = blockedDayService.getAllExceptions();
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Retorna apenas exceções futuras (a partir de hoje).
     *
     * @return Lista de exceções futuras (200 OK)
     */
    @GetMapping("/future")
    public ResponseEntity<List<BlockedDayExceptionEntity>> getFutureExceptions() {
        List<BlockedDayExceptionEntity> exceptions = blockedDayService.getFutureExceptions();
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Remove uma exceção existente.
     *
     * @param exceptionId ID da exceção a remover
     * @return Resposta vazia (204 No Content)
     */
    @DeleteMapping("/{exceptionId}")
    public ResponseEntity<Void> deleteException(@PathVariable UUID exceptionId) {
        blockedDayService.deleteException(exceptionId);
        return ResponseEntity.noContent().build();
    }
}

