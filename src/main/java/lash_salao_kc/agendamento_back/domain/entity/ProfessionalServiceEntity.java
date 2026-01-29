package lash_salao_kc.agendamento_back.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa o vínculo entre Profissional e Serviço.
 * Define quais serviços cada profissional está habilitado a executar.
 *
 * Um profissional pode executar vários serviços.
 * Um serviço pode ser executado por vários profissionais.
 *
 * Tabela: tb_professional_services
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "tb_professional_services",
    uniqueConstraints = @UniqueConstraint(columnNames = {"professional_id", "service_id"})
)
public class ProfessionalServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    /**
     * Profissional que executa o serviço.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private ProfessionalEntity professional;

    /**
     * Serviço que o profissional pode executar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServicesEntity service;

    /**
     * Data/hora de criação do vínculo.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

