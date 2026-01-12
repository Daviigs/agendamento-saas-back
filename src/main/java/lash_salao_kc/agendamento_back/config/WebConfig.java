package lash_salao_kc.agendamento_back.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de interceptors Web MVC.
 * Registra o TenantInterceptor para validação de multi-tenancy em todas as requisições.
 *
 * Endpoints excluídos da validação:
 * - /h2-console/** - Console H2 Database
 * - /static/** - Recursos estáticos
 * - /error - Páginas de erro do Spring
 * - /swagger-ui/** - Documentação Swagger
 * - /v3/api-docs/** - OpenAPI docs
 * - /actuator/** - Spring Actuator (se habilitado)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/h2-console/**",
                        "/static/**",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );
    }
}

