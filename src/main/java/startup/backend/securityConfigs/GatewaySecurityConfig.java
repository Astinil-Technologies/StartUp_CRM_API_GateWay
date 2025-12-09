package startup.backend.securityConfigs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         AuthenticationWebFilter authenticationWebFilter) {

        return http
                // ❌ Disable CSRF (you’re using tokens)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // ✅ Enable CORS for Angular
                .cors(Customizer.withDefaults())
                // 🚫 Disable default login mechanisms (fixes browser popup)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // ✅ Authorization rules
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/auth/**",
                                "/api/chatbot/**",
                                "/api/v1/users/request-password-reset",
                                "/api/v1/users/reset-password"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                // ✅ Make security stateless
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // ✅ Add your JWT filter
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
