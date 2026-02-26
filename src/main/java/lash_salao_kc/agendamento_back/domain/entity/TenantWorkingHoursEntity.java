package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidade que representa o horário de funcionamento do TENANT.
 * O horário é único por tenant — todos os profissionais compartilham o mesmo expediente.
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
     * ID do tenant ao qual o horário de funcionamento pertence.
     * Este é o identificador principal — o horário é do tenant.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Coluna mantida por compatibilidade com o schema existente.
     * NÃO deve ser usada para diferenciar horários — deve ser sempre NULL.
     * O horário de funcionamento é do TENANT, não do profissional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = true)
    private ProfessionalEntity professional;

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

    /**
     * Define o comportamento de validação de agendamentos em relação a bloqueios e horários.
     *
     * - true (Flexível): Agendamentos podem ultrapassar bloqueios e o horário final.
     *   Útil para negócios com agenda flexível (ex: salões, prestadores autônomos).
     *
     * - false (Rígido): Bloqueios e horário final são barreiras absolutas.
     *   Útil para negócios com agenda rígida (ex: clínicas, consultórios).
     *
     * Padrão: false (mais restritivo)
     */
    @NotNull
    @Column(name = "horario_flexivel", nullable = false)
    private Boolean horarioFlexivel = false;
}

