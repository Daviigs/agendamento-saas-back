package lash_salao_kc.agendamento_back.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um Profissional vinculado a um Tenant.
 * Cada profissional possui horários, bloqueios e agendamentos independentes.
 *
 * Tabela: tb_professionals
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_professionals")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "professional_id")
    private UUID id;

    /**
     * Tenant ao qual o profissional pertence.
     */
    @NotNull(message = "Tenant é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    /**
     * Nome do profissional.
     */
    @NotBlank(message = "Nome do profissional é obrigatório")
    @Column(name = "professional_name", nullable = false)
    private String professionalName;

    /**
     * Email do profissional.
     */
    @Email(message = "Email inválido")
    @Column(name = "professional_email")
    private String professionalEmail;

    /**
     * Telefone do profissional.
     */
    @Column(name = "professional_phone", length = 20)
    private String professionalPhone;

    /**
     * Indica se o profissional está ativo.
     * Profissionais inativos não podem receber novos agendamentos.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Data/hora de criação do registro.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data/hora da última atualização do registro.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Método auxiliar para obter o tenant_id como UUID.
     */
    @Transient
    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }
}

