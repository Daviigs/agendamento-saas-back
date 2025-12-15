package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "tb_services")
public class ServicesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "service_id")
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    private int duration;

    @NotNull
    private double price;
}
