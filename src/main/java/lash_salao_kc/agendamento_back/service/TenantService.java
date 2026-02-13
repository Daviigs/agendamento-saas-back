package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.dto.CreateTenantRequest;
import lash_salao_kc.agendamento_back.domain.dto.UpdateTenantRequest;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.DuplicateResourceException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de tenants (clientes multi-tenant).
 * Busca tenants persistidos no banco de dados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Retorna lista de todos os tenants ativos no sistema.
     * Busca do banco de dados.
     *
     * @return Lista de IDs (tenant_key) dos tenants ativos
     */
    public List<String> getAllActiveTenants() {
        return tenantRepository.findByActiveTrue().stream()
                .map(TenantEntity::getTenantKey)
                .collect(Collectors.toList());
    }

    /**
     * Busca um tenant pela chave (tenant_key).
     *
     * @param tenantKey Chave do tenant (ex: "kc", "mjs")
     * @return TenantEntity encontrado
     * @throws ResourceNotFoundException se não encontrado
     */
    public TenantEntity getTenantByKey(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantKey));
    }

    /**
     * Busca um tenant ativo pela chave.
     *
     * @param tenantKey Chave do tenant
     * @return TenantEntity ativo
     * @throws BusinessException se o tenant não existir ou não estiver ativo
     */
    public TenantEntity getActiveTenantByKey(String tenantKey) {
        return tenantRepository.findByTenantKeyAndActiveTrue(tenantKey)
                .orElseThrow(() -> new BusinessException(
                        String.format("Tenant '%s' não encontrado ou inativo", tenantKey)));
    }

    /**
     * Busca um tenant por ID.
     *
     * @param tenantId ID do tenant
     * @return TenantEntity encontrado
     * @throws ResourceNotFoundException se não encontrado
     */
    public TenantEntity getTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }

    /**
     * Lista todos os tenants (ativos e inativos).
     *
     * @return Lista de tenants
     */
    public List<TenantEntity> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Cria um novo tenant.
     *
     * @param request Dados do tenant
     * @return Tenant criado
     * @throws DuplicateResourceException se já existir um tenant com a mesma chave
     */
    @Transactional
    public TenantEntity createTenant(CreateTenantRequest request) {
        String tenantKey = request.getTenantKey().toLowerCase().trim();

        if (tenantRepository.existsByTenantKey(tenantKey)) {
            throw new DuplicateResourceException(
                    String.format("Já existe um tenant com a chave '%s'", tenantKey));
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantKey(tenantKey);
        tenant.setBusinessName(request.getBusinessName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setActive(true);

        // Define tempo de lembrete: usa o valor do request ou mantém o padrão (120 minutos)
        if (request.getTempoLembreteMinutos() != null) {
            tenant.setTempoLembreteMinutos(request.getTempoLembreteMinutos());
        }

        TenantEntity saved = tenantRepository.save(tenant);
        log.info("Novo tenant criado: {} - {} (lembrete: {} minutos antes)",
                saved.getTenantKey(), saved.getBusinessName(), saved.getTempoLembreteMinutos());

        return saved;
    }

    /**
     * Atualiza um tenant existente (versão antiga - manter para compatibilidade).
     *
     * @param tenantId ID do tenant
     * @param request  Dados atualizados
     * @return Tenant atualizado
     */
    @Transactional
    public TenantEntity updateTenant(UUID tenantId, CreateTenantRequest request) {
        log.info("🔍 DEBUG - Iniciando atualização do tenant: {}", tenantId);
        log.info("🔍 DEBUG - Request recebido: businessName={}, email={}, phone={}, tempoLembrete={}",
            request.getBusinessName(),
            request.getContactEmail(),
            request.getContactPhone(),
            request.getTempoLembreteMinutos());

        TenantEntity tenant = getTenantById(tenantId);

        log.info("🔍 DEBUG - Tenant atual: key={}, businessName={}, tempoLembrete={}",
            tenant.getTenantKey(),
            tenant.getBusinessName(),
            tenant.getTempoLembreteMinutos());

        tenant.setBusinessName(request.getBusinessName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());

        // Atualiza tempo de lembrete se fornecido
        if (request.getTempoLembreteMinutos() != null) {
            log.info("🔍 DEBUG - Atualizando tempo de lembrete de {} para {}",
                tenant.getTempoLembreteMinutos(), request.getTempoLembreteMinutos());
            tenant.setTempoLembreteMinutos(request.getTempoLembreteMinutos());
            log.info("🔍 DEBUG - Tempo após setTempoLembreteMinutos: {}", tenant.getTempoLembreteMinutos());
        } else {
            log.warn("⚠️ DEBUG - tempoLembreteMinutos está NULL no request!");
        }

        log.info("🔍 DEBUG - Salvando tenant com tempoLembrete={}", tenant.getTempoLembreteMinutos());
        TenantEntity saved = tenantRepository.save(tenant);
        log.info("🔍 DEBUG - Tenant salvo: tempoLembrete={}, updatedAt={}",
            saved.getTempoLembreteMinutos(), saved.getUpdatedAt());

        log.info("Tenant atualizado: {}", tenant.getTenantKey());
        return saved;
    }

    /**
     * Atualiza um tenant existente usando UpdateTenantRequest (recomendado).
     * Apenas os campos não-null no request serão atualizados.
     *
     * @param tenantId ID do tenant
     * @param request  Dados atualizados
     * @return Tenant atualizado
     */
    @Transactional
    public TenantEntity updateTenant(UUID tenantId, UpdateTenantRequest request) {
        log.info("🔍 DEBUG - Iniciando atualização do tenant: {}", tenantId);
        log.info("🔍 DEBUG - UpdateRequest recebido: businessName={}, email={}, phone={}, tempoLembrete={}",
            request.getBusinessName(),
            request.getContactEmail(),
            request.getContactPhone(),
            request.getTempoLembreteMinutos());

        TenantEntity tenant = getTenantById(tenantId);

        log.info("🔍 DEBUG - Tenant atual: key={}, businessName={}, tempoLembrete={}",
            tenant.getTenantKey(),
            tenant.getBusinessName(),
            tenant.getTempoLembreteMinutos());

        // Atualiza apenas os campos que foram enviados (não-null)
        if (request.getBusinessName() != null) {
            log.info("🔍 DEBUG - Atualizando businessName de '{}' para '{}'",
                tenant.getBusinessName(), request.getBusinessName());
            tenant.setBusinessName(request.getBusinessName());
        }

        if (request.getContactEmail() != null) {
            log.info("🔍 DEBUG - Atualizando email");
            tenant.setContactEmail(request.getContactEmail());
        }

        if (request.getContactPhone() != null) {
            log.info("🔍 DEBUG - Atualizando telefone");
            tenant.setContactPhone(request.getContactPhone());
        }

        // Atualiza tempo de lembrete se fornecido
        if (request.getTempoLembreteMinutos() != null) {
            log.info("🔍 DEBUG - Atualizando tempo de lembrete de {} para {}",
                tenant.getTempoLembreteMinutos(), request.getTempoLembreteMinutos());
            tenant.setTempoLembreteMinutos(request.getTempoLembreteMinutos());
        }

        log.info("🔍 DEBUG - Salvando tenant com tempoLembrete={}", tenant.getTempoLembreteMinutos());
        TenantEntity saved = tenantRepository.save(tenant);
        log.info("🔍 DEBUG - Tenant salvo: tempoLembrete={}, updatedAt={}",
            saved.getTempoLembreteMinutos(), saved.getUpdatedAt());

        log.info("Tenant atualizado: {}", tenant.getTenantKey());
        return saved;
    }

    /**
     * Ativa ou desativa um tenant.
     *
     * @param tenantId ID do tenant
     * @param active   true para ativar, false para desativar
     * @return Tenant atualizado
     */
    @Transactional
    public TenantEntity setTenantActive(UUID tenantId, boolean active) {
        TenantEntity tenant = getTenantById(tenantId);
        tenant.setActive(active);

        log.info("Tenant {} {}", tenant.getTenantKey(), active ? "ativado" : "desativado");
        return tenantRepository.save(tenant);
    }

    /**
     * Obtém o tenant do contexto da requisição atual.
     *
     * @return TenantEntity do contexto
     */
    public TenantEntity getCurrentTenant() {
        String tenantKey = TenantContext.getTenantId();
        return getActiveTenantByKey(tenantKey);
    }
}

