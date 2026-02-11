package lash_salao_kc.agendamento_back.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request para criar uma exceção de bloqueio recorrente.
 * Permite liberar uma data específica que cai em um dia bloqueado recorrente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlockedDayExceptionRequest {

    @NotNull(message = "Data é obrigatória")
    private LocalDate exceptionDate;

    @NotNull(message = "Motivo é obrigatório")
    private String reason;
}

