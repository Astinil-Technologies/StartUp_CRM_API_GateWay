package startup.backend.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import startup.backend.util.JwtTokenUtil;


@Component
public class AuthorizationHeaderForwardingFilter implements GlobalFilter, Ordered {

    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public AuthorizationHeaderForwardingFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // just check token is expired and token format ,if both valid you can forward token to microservices
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/auth/") ||
                path.startsWith("/api/v1/users/request-password-reset") ||
                path.startsWith("/api/v1/users/reset-password")) {
            return chain.filter(exchange); // Skip JWT check
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if(!isValidJwtFormat(token) || jwtTokenUtil.isTokenExpired(token))
        {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        System.out.println("Token is validated at gateway and forward to microservices..");

        ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());

    }

    @Override
    public int getOrder() {
        return -1; // run early
    }

    private boolean isValidJwtFormat(String token) {
        return token.chars().filter(ch -> ch == '.').count() == 2;
    }
}
