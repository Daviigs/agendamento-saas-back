package lash_salao_kc.agendamento_back.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um Tenant (salão/cliente) no sistema multi-tenant.
 * Cada tenant é um cliente do sistema que possui seus próprios dados isolados.
 *
 * Tabela: tb_tenants
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_tenants")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tenant_id")
    private UUID id;

    /**
     * Chave única do tenant usada no header HTTP X-Tenant-Id.
     * Formato: lowercase, alfanumérico com hífens/underscores (ex: kc, mjs, salao-bella)
     */
    @NotBlank(message = "Chave do tenant é obrigatória")
    @Pattern(regexp = "^[a-z0-9\\-_]+$", message = "Chave do tenant deve conter apenas letras minúsculas, números, hífens e underscores")
    @Column(name = "tenant_key", nullable = false, unique = true, length = 50)
    private String tenantKey;

    /**
     * Nome comercial do salão/empresa.
     */
    @NotBlank(message = "Nome comercial é obrigatório")
    @Column(name = "business_name", nullable = false)
    private String businessName;

    /**
     * Email de contato do tenant.
     */
    @Email(message = "Email inválido")
    @Column(name = "contact_email")
    private String contactEmail;

    /**
     * Telefone de contato do tenant.
     */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /**
     * Indica se o tenant está ativo no sistema.
     * Tenants inativos não podem realizar operações.
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
}

