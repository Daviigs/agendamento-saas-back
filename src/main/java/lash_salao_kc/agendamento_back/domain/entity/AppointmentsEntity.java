package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_appointments")
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
     * Método auxiliar para manter compatibilidade com código antigo
     * Retorna o primeiro serviço da lista (para casos de serviço único)
     */
    @Deprecated
    public ServicesEntity getService() {
        return services.isEmpty() ? null : services.get(0);
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
