package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "tb_services")
public class ServicesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "service_id")
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    private String name;

    @NotNull
    private int duration;

    @NotNull
    private double price;
}
