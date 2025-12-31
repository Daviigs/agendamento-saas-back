package lash_salao_kc.agendamento_back.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que extrai o tenant (cliente) do header X-Client-Id
 * e injeta no TenantContext para uso durante toda a requisição
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Client-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);

        // Se não veio header, usa "default" (retrocompatibilidade com front-end atual)
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default";
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

