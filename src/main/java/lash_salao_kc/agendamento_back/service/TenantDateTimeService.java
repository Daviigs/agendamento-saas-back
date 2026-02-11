package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Serviço utilitário para operações de data/hora considerando o timezone do tenant.
 * Usado para validar agendamentos no passado e filtrar horários disponíveis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDateTimeService {

    private final TenantRepository tenantRepository;

    /**
     * Retorna a data/hora atual no timezone do tenant atual (do contexto).
     *
     * @return ZonedDateTime atual no timezone do tenant
     */
    public ZonedDateTime now() {
        String tenantId = TenantContext.getTenantId();
        return now(tenantId);
    }

    /**
     * Retorna a data/hora atual no timezone de um tenant específico.
     *
     * @param tenantId ID do tenant
     * @return ZonedDateTime atual no timezone do tenant
     */
    public ZonedDateTime now(String tenantId) {
        ZoneId zoneId = getTenantZoneId(tenantId);
        return ZonedDateTime.now(zoneId);
    }

    /**
     * Retorna a data atual (sem hora) no timezone do tenant atual.
     *
     * @return LocalDate atual no timezone do tenant
     */
    public LocalDate today() {
        return now().toLocalDate();
    }

    /**
     * Retorna a data atual (sem hora) no timezone de um tenant específico.
     *
     * @param tenantId ID do tenant
     * @return LocalDate atual no timezone do tenant
     */
    public LocalDate today(String tenantId) {
        return now(tenantId).toLocalDate();
    }

    /**
     * Converte uma data e hora local para ZonedDateTime no timezone do tenant.
     *
     * @param date Data local
     * @param time Hora local
     * @return ZonedDateTime no timezone do tenant
     */
    public ZonedDateTime toZonedDateTime(LocalDate date, LocalTime time) {
        String tenantId = TenantContext.getTenantId();
        return toZonedDateTime(date, time, tenantId);
    }

    /**
     * Converte uma data e hora local para ZonedDateTime no timezone de um tenant específico.
     *
     * @param date Data local
     * @param time Hora local
     * @param tenantId ID do tenant
     * @return ZonedDateTime no timezone do tenant
     */
    public ZonedDateTime toZonedDateTime(LocalDate date, LocalTime time, String tenantId) {
        ZoneId zoneId = getTenantZoneId(tenantId);
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        return localDateTime.atZone(zoneId);
    }

    /**
     * Verifica se uma data/hora está no passado (antes do momento atual) considerando o timezone do tenant.
     *
     * @param date Data a verificar
     * @param time Hora a verificar
     * @return true se está no passado
     */
    public boolean isInPast(LocalDate date, LocalTime time) {
        String tenantId = TenantContext.getTenantId();
        return isInPast(date, time, tenantId);
    }

    /**
     * Verifica se uma data/hora está no passado (antes do momento atual) considerando o timezone de um tenant.
     *
     * @param date Data a verificar
     * @param time Hora a verificar
     * @param tenantId ID do tenant
     * @return true se está no passado
     */
    public boolean isInPast(LocalDate date, LocalTime time, String tenantId) {
        ZonedDateTime appointmentDateTime = toZonedDateTime(date, time, tenantId);
        ZonedDateTime now = now(tenantId);

        // Considera "no passado" se for anterior OU igual ao momento atual
        // Isso impede agendamentos no minuto exato atual
        return appointmentDateTime.isBefore(now) || appointmentDateTime.equals(now);
    }

    /**
     * Verifica se uma data está no passado (antes da data atual) considerando o timezone do tenant.
     *
     * @param date Data a verificar
     * @return true se está no passado
     */
    public boolean isDateInPast(LocalDate date) {
        String tenantId = TenantContext.getTenantId();
        return isDateInPast(date, tenantId);
    }

    /**
     * Verifica se uma data está no passado (antes da data atual) considerando o timezone de um tenant.
     *
     * @param date Data a verificar
     * @param tenantId ID do tenant
     * @return true se está no passado
     */
    public boolean isDateInPast(LocalDate date, String tenantId) {
        LocalDate today = today(tenantId);
        return date.isBefore(today);
    }

    /**
     * Verifica se uma data é a data atual no timezone do tenant.
     *
     * @param date Data a verificar
     * @return true se é hoje
     */
    public boolean isToday(LocalDate date) {
        String tenantId = TenantContext.getTenantId();
        return isToday(date, tenantId);
    }

    /**
     * Verifica se uma data é a data atual no timezone de um tenant específico.
     *
     * @param date Data a verificar
     * @param tenantId ID do tenant
     * @return true se é hoje
     */
    public boolean isToday(LocalDate date, String tenantId) {
        LocalDate today = today(tenantId);
        return date.equals(today);
    }

    /**
     * Obtém o ZoneId do timezone do tenant.
     *
     * @param tenantId ID do tenant
     * @return ZoneId do timezone do tenant
     * @throws BusinessException se o tenant não for encontrado ou o timezone for inválido
     */
    public ZoneId getTenantZoneId(String tenantId) {
        TenantEntity tenant = tenantRepository.findByTenantKeyAndActiveTrue(tenantId)
                .orElseThrow(() -> new BusinessException(
                        String.format("Tenant '%s' não encontrado ou inativo", tenantId)));

        String timezone = tenant.getTimezone();
        if (timezone == null || timezone.trim().isEmpty()) {
            timezone = "America/Sao_Paulo"; // Fallback para o padrão
            log.warn("Tenant {} sem timezone configurado, usando padrão: {}", tenantId, timezone);
        }

        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.error("Timezone inválido '{}' para tenant {}. Usando padrão America/Sao_Paulo",
                    timezone, tenantId, e);
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    /**
     * Obtém a hora atual (sem data) no timezone do tenant.
     *
     * @return LocalTime atual no timezone do tenant
     */
    public LocalTime currentTime() {
        return now().toLocalTime();
    }

    /**
     * Obtém a hora atual (sem data) no timezone de um tenant específico.
     *
     * @param tenantId ID do tenant
     * @return LocalTime atual no timezone do tenant
     */
    public LocalTime currentTime(String tenantId) {
        return now(tenantId).toLocalTime();
    }
}

