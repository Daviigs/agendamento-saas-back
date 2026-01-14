package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidade que representa o horário de trabalho de cada profissional (tenant).
 * Cada tenant possui seu próprio horário de funcionamento.
 *
 * Exemplos:
 * - kc: 09:00 às 18:00
 * - mjs: 07:00 às 16:00
 *
 * Tabela: tb_tenant_working_hours
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_tenant_working_hours")
public class TenantWorkingHoursEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "working_hours_id")
    private UUID id;

    /**
     * ID do tenant (profissional/colaborador).
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    /**
     * Horário de início do expediente.
     */
    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Horário de término do expediente.
     */
    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Intervalo entre slots de agendamento (em minutos).
     * Padrão: 30 minutos.
     */
    @NotNull
    @Column(name = "slot_interval_minutes", nullable = false)
    private Integer slotIntervalMinutes = 30;

    /**
     * Indica se o tenant está ativo no sistema.
     */
    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}

