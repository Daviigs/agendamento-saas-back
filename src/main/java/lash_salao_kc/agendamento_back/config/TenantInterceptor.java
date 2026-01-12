package lash_salao_kc.agendamento_back.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lash_salao_kc.agendamento_back.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Interceptor que extrai o tenant (cliente) do header X-Client-Id
 * e injeta no TenantContext para uso durante toda a requisição.
 *
 * SEGURANÇA:
 * - Bloqueia requisições sem header X-Client-Id
 * - Valida se o tenant está autorizado no sistema
 *
 * EXCEÇÕES (não exigem header):
 * - Requisições OPTIONS (CORS preflight)
 * - Requisições para /error (páginas de erro)
 * - Paths excluídos no WebConfig (swagger, h2-console, etc)
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Client-Id";

    @Autowired
    private TenantService tenantService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Permite requisições OPTIONS (CORS preflight) sem header
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Permite requisições para /error sem header
        if (request.getRequestURI().startsWith("/error")) {
            return true;
        }

        String tenantId = request.getHeader(TENANT_HEADER);

        // VALIDAÇÃO 1: Header X-Client-Id é obrigatório
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error("❌ Requisição bloqueada: Header X-Client-Id ausente | Endpoint: {} {}",
                request.getMethod(), request.getRequestURI());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Header X-Client-Id é obrigatório"
            );
        }

        // Normaliza para minúsculas (kc, mjs)
        tenantId = tenantId.toLowerCase().trim();

        // VALIDAÇÃO 2: Tenant deve estar na lista de autorizados
        List<String> validTenants = tenantService.getAllActiveTenants();
        if (!validTenants.contains(tenantId)) {
            logger.error("❌ Requisição bloqueada: Tenant '{}' não autorizado | Endpoint: {} {}",
                tenantId, request.getMethod(), request.getRequestURI());
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                String.format("Tenant '%s' não autorizado. Tenants válidos: %s",
                    tenantId, validTenants)
            );
        }

        // ✅ Validações passaram - Tenant autorizado
        logger.info("🔑 Tenant autorizado: {} | Endpoint: {} {}",
            tenantId, request.getMethod(), request.getRequestURI());

        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Limpa o contexto após a requisição para evitar memory leak
        TenantContext.clear();
    }
}

