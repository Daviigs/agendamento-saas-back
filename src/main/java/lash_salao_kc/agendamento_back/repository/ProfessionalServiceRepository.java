package lash_salao_kc.agendamento_back.repository;

import lash_salao_kc.agendamento_back.domain.entity.ProfessionalServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para acesso a vínculos entre profissionais e serviços.
 */
@Repository
public interface ProfessionalServiceRepository extends JpaRepository<ProfessionalServiceEntity, UUID> {

    /**
     * Busca todos os vínculos de um profissional.
     *
     * @param professionalId ID do profissional
     * @return Lista de vínculos
     */
    List<ProfessionalServiceEntity> findByProfessionalId(UUID professionalId);

    /**
     * Busca todos os vínculos de um serviço.
     *
     * @param serviceId ID do serviço
     * @return Lista de vínculos
     */
    List<ProfessionalServiceEntity> findByServiceId(UUID serviceId);

    /**
     * Verifica se existe um vínculo específico.
     *
     * @param professionalId ID do profissional
     * @param serviceId      ID do serviço
     * @return true se o vínculo existe
     */
    boolean existsByProfessionalIdAndServiceId(UUID professionalId, UUID serviceId);

    /**
     * Busca um vínculo específico.
     *
     * @param professionalId ID do profissional
     * @param serviceId      ID do serviço
     * @return Optional com o vínculo se existir
     */
    Optional<ProfessionalServiceEntity> findByProfessionalIdAndServiceId(UUID professionalId, UUID serviceId);

    /**
     * Remove todos os vínculos de um profissional.
     *
     * @param professionalId ID do profissional
     */
    @Modifying
    @Query("DELETE FROM ProfessionalServiceEntity ps WHERE ps.professional.id = :professionalId")
    void deleteByProfessionalId(@Param("professionalId") UUID professionalId);

    /**
     * Remove todos os vínculos de um serviço.
     *
     * @param serviceId ID do serviço
     */
    @Modifying
    @Query("DELETE FROM ProfessionalServiceEntity ps WHERE ps.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") UUID serviceId);

    /**
     * Busca IDs de serviços vinculados a um profissional.
     *
     * @param professionalId ID do profissional
     * @return Lista de UUIDs dos serviços
     */
    @Query("SELECT ps.service.id FROM ProfessionalServiceEntity ps WHERE ps.professional.id = :professionalId")
    List<UUID> findServiceIdsByProfessionalId(@Param("professionalId") UUID professionalId);

    /**
     * Busca IDs de profissionais vinculados a um serviço.
     *
     * @param serviceId ID do serviço
     * @return Lista de UUIDs dos profissionais
     */
    @Query("SELECT ps.professional.id FROM ProfessionalServiceEntity ps WHERE ps.service.id = :serviceId")
    List<UUID> findProfessionalIdsByServiceId(@Param("serviceId") UUID serviceId);

    /**
     * Verifica se um profissional executa TODOS os serviços da lista.
     *
     * @param professionalId ID do profissional
     * @param serviceIds     Lista de IDs de serviços
     * @param count          Quantidade de serviços (deve ser igual ao resultado)
     * @return true se o profissional executa todos os serviços
     */
    @Query("SELECT COUNT(DISTINCT ps.service.id) = :count " +
           "FROM ProfessionalServiceEntity ps " +
           "WHERE ps.professional.id = :professionalId " +
           "AND ps.service.id IN :serviceIds")
    boolean professionalExecutesAllServices(
        @Param("professionalId") UUID professionalId,
        @Param("serviceIds") List<UUID> serviceIds,
        @Param("count") long count
    );

    /**
     * Busca profissionais que executam TODOS os serviços da lista.
     * Usado para filtrar profissionais disponíveis baseado nos serviços selecionados.
     *
     * @param serviceIds Lista de IDs de serviços
     * @param tenantId   ID do tenant
     * @param count      Quantidade de serviços
     * @return Lista de IDs de profissionais que executam todos os serviços
     */
    @Query("SELECT ps.professional.id " +
           "FROM ProfessionalServiceEntity ps " +
           "WHERE ps.service.id IN :serviceIds " +
           "AND ps.professional.tenant.id = :tenantId " +
           "AND ps.professional.active = true " +
           "GROUP BY ps.professional.id " +
           "HAVING COUNT(DISTINCT ps.service.id) = :count")
    List<UUID> findProfessionalIdsByAllServices(
        @Param("serviceIds") List<UUID> serviceIds,
        @Param("tenantId") UUID tenantId,
        @Param("count") long count
    );
}

