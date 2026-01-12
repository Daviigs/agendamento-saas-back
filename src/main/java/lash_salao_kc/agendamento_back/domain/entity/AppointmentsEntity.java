package lash_salao_kc.agendamento_back.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um agendamento no sistema.
 * Suporta múltiplos serviços por agendamento.
 *
 * Tabela: tb_appointments
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_appointments")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "appointment_id")
    private UUID id;

    /**
     * ID do tenant (cliente multi-tenant) dono deste agendamento.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Data do agendamento.
     */
    @NotNull
    @Column(name = "appointment_date", nullable = false)
    private LocalDate date;

    /**
     * Horário de início do agendamento.
     */
    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Horário de término do agendamento (calculado pela soma das durações dos serviços).
     */
    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Lista de serviços incluídos neste agendamento.
     * Relacionamento Many-to-Many com ServicesEntity.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tb_appointment_services",
        joinColumns = @JoinColumn(name = "appointment_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @JsonProperty("services")
    private List<ServicesEntity> services = new ArrayList<>();

    /**
     * Nome do cliente que fez o agendamento.
     */
    @NotNull
    @Column(name = "user_name", nullable = false)
    private String userName;

    /**
     * Telefone do cliente (usado para envio de notificações via WhatsApp).
     */
    @NotNull
    @Column(name = "user_phone", nullable = false)
    private String userPhone;

    /**
     * Flag que indica se o lembrete automático já foi enviado.
     * Evita envio duplicado de lembretes.
     */
    @Column(nullable = false)
    private boolean reminderSent = false;

    /**
     * Getter que garante que a lista de serviços nunca seja null.
     */
    public List<ServicesEntity> getServices() {
        if (services == null) {
            services = new ArrayList<>();
        }
        return services;
    }

    /**
     * Método auxiliar para compatibilidade com código legado.
     * Retorna o primeiro serviço da lista (para casos de serviço único).
     *
     * @deprecated Use getServices() para acessar todos os serviços
     */
    @Deprecated
    public ServicesEntity getService() {
        return getServices().isEmpty() ? null : getServices().get(0);
    }

    /**
     * Método auxiliar para compatibilidade com código legado.
     * Adiciona um serviço único à lista.
     *
     * @deprecated Use getServices().add() para adicionar serviços
     */
    @Deprecated
    public void setService(ServicesEntity service) {
        this.services = new ArrayList<>();
        if (service != null) {
            this.services.add(service);
        }
    }
}
