package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidade que representa bloqueios de horários específicos (não o dia inteiro).
 * Suporta dois tipos de bloqueios:
 * 1. Bloqueio de intervalo de tempo em uma data específica (ex: 14:00-16:00 em 25/12/2025)
 * 2. Bloqueio recorrente por dia da semana (ex: 16:00-17:00 todas as segundas-feiras)
 *
 * Tabela: tb_blocked_time_slots
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_blocked_time_slots")
public class BlockedTimeSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "blocked_slot_id")
    private UUID id;

    /**
     * ID do tenant (profissional) dono deste bloqueio.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Data específica do bloqueio (para bloqueios pontuais).
     * Null se for um bloqueio recorrente.
     */
    @Column(name = "specific_date")
    private LocalDate specificDate;

    /**
     * Dia da semana do bloqueio (para bloqueios recorrentes).
     * Null se for um bloqueio de data específica.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    /**
     * Horário de início do bloqueio.
     */
    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Horário de término do bloqueio.
     */
    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Motivo do bloqueio.
     * Exemplos: "Almoço", "Reunião", "Intervalo"
     */
    @NotNull
    @Column(name = "reason", nullable = false)
    private String reason;

    /**
     * Indica o tipo de bloqueio:
     * - true: bloqueio recorrente (toda segunda, toda terça, etc)
     * - false: bloqueio de data específica
     */
    @NotNull
    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;
}

