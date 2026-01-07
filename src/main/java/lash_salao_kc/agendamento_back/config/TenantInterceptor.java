package lash_salao_kc.agendamento_back.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que extrai o tenant (cliente) do header X-Client-Id
 * e injeta no TenantContext para uso durante toda a requisição
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Client-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);

        // Se não veio header, usa "default" (retrocompatibilidade com front-end atual)
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default";
            logger.warn("⚠️  Header X-Client-Id não encontrado, usando tenant 'default'");
        } else {
            // Normaliza para minúsculas (kc, mjs)
            tenantId = tenantId.toLowerCase().trim();
            logger.info("🔑 Tenant detectado: {} | Endpoint: {} {}", tenantId, request.getMethod(), request.getRequestURI());
        }

        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Limpa o contexto após a requisição para evitar memory leak
        TenantContext.clear();
    }
}

