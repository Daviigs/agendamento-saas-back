package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade que representa dias bloqueados para agendamentos.
 * Suporta dois tipos de bloqueios:
 * 1. Bloqueio de data específica (ex: feriados)
 * 2. Bloqueio recorrente por dia da semana (ex: todos os domingos)
 *
 * Tabela: tb_blocked_days
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_blocked_days")
public class BlockedDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "blocked_day_id")
    private UUID id;

    /**
     * ID do tenant (cliente multi-tenant) dono deste bloqueio.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Data específica bloqueada (ex: 25/12/2025 - Natal).
     * Null se for um bloqueio recorrente por dia da semana.
     */
    @Column(name = "specific_date")
    private LocalDate specificDate;

    /**
     * Dia da semana bloqueado de forma recorrente (ex: SUNDAY - Todo domingo).
     * Null se for um bloqueio de data específica.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    /**
     * Motivo do bloqueio.
     * Exemplos: "Feriado - Natal", "Folga semanal"
     */
    @NotNull
    @Column(name = "reason", nullable = false)
    private String reason;

    /**
     * Indica o tipo de bloqueio:
     * - true: bloqueio recorrente (todo domingo, toda segunda, etc)
     * - false: bloqueio de data específica
     */
    @NotNull
    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;
}

