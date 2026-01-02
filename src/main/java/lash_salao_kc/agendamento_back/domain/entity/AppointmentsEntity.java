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

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "appointment_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tb_appointment_services",
        joinColumns = @JoinColumn(name = "appointment_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @JsonProperty("services")
    private List<ServicesEntity> services = new ArrayList<>();

    @NotNull
    @Column(name = "user_name", nullable = false)
    private String userName;

    @NotNull
    @Column(name = "user_phone", nullable = false)
    private String userPhone;

    @Column(nullable = false)
    private boolean reminderSent = false;

    /**
     * Getter personalizado para garantir que services nunca seja null
     */
    public List<ServicesEntity> getServices() {
        if (services == null) {
            services = new ArrayList<>();
        }
        return services;
    }

    /**
     * Método auxiliar para manter compatibilidade com código antigo
     * Retorna o primeiro serviço da lista (para casos de serviço único)
     */
    @Deprecated
    public ServicesEntity getService() {
        return getServices().isEmpty() ? null : getServices().get(0);
    }

    /**
     * Método auxiliar para manter compatibilidade com código antigo
     * Adiciona um serviço único à lista
     */
    @Deprecated
    public void setService(ServicesEntity service) {
        this.services = new ArrayList<>();
        if (service != null) {
            this.services.add(service);
        }
    }
}
