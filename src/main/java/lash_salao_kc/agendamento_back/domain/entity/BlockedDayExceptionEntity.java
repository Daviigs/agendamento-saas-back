package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade que representa exceções para dias bloqueados recorrentes.
 * Permite liberar uma data específica que cai em um dia bloqueado recorrente.
 *
 * Exemplo de uso:
 * - Todos os domingos são bloqueados (recorrente)
 * - Mas o dono quer trabalhar no domingo 15/02/2026
 * - Cria-se uma exceção para essa data específica
 *
 * Tabela: tb_blocked_day_exceptions
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_blocked_day_exceptions")
public class BlockedDayExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "exception_id")
    private UUID id;

    /**
     * ID do tenant (cliente multi-tenant) dono desta exceção.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Data específica liberada como exceção.
     * Esta data terá prioridade sobre bloqueios recorrentes.
     */
    @NotNull
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    /**
     * Motivo da liberação.
     * Exemplos: "Trabalho extra", "Reposição", "Evento especial"
     */
    @NotNull
    @Column(name = "reason", nullable = false)
    private String reason;
}

