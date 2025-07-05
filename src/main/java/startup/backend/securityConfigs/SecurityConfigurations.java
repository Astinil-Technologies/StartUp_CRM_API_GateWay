package startup.backend.securityConfigs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import startup.backend.util.JwtTokenUtil;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfigurations {


    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(JwtTokenUtil jwtUtil) {
        return authentication -> {
            String token = authentication.getCredentials().toString();

            if (!jwtUtil.isValidJwtFormat(token) || jwtUtil.isTokenExpired(token)) {
                return Mono.error(new BadCredentialsException("Invalid or expired token"));
            }

            String username = jwtUtil.extractUsername(token);
            List<GrantedAuthority> authorities = new ArrayList<>(); // You can populate roles here if needed

            return Mono.just(new UsernamePasswordAuthenticationToken(username, token, authorities));
        };
    }



    @Bean
    public AuthenticationWebFilter authenticationWebFilter(ReactiveAuthenticationManager authManager) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authManager);

        filter.setServerAuthenticationConverter(exchange -> {
            String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
            }
            return Mono.empty(); // No token = unauthenticated
        });

        filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        return filter;
    }


}
