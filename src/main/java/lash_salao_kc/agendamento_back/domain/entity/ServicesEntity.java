package lash_salao_kc.agendamento_back.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidade que representa um serviço oferecido pelo salão.
 * Cada serviço tem nome, duração (em minutos) e preço.
 *
 * Tabela: tb_services
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tb_services")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "service_id")
    private UUID id;

    /**
     * ID do tenant (cliente multi-tenant) dono deste serviço.
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Nome do serviço (ex: "Design de Sobrancelhas", "Aplicação de Cílios").
     */
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Duração do serviço em minutos.
     */
    @NotNull
    @Column(name = "duration", nullable = false)
    private int duration;

    /**
     * Preço do serviço em reais.
     */
    @NotNull
    @Column(name = "price", nullable = false)
    private double price;
}
