package startup.backend.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import startup.backend.exception.JwtTokenException;
import startup.backend.exception.JwtTokenExpiredException;
import startup.backend.exception.JwtTokenParseException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
 import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    private SecretKey secretKey;

    @Value("${jwt.secretKey}")
    private String secretKeyString;

    @Value("${jwt.expirationMs}")
    private Long expirationMs;

    private static final long ALLOWED_CLOCK_SKEW = 5 * 60 * 1000;

    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            long currentTime = System.currentTimeMillis();
            long tokenExpirationTime = expiration.getTime();
            return tokenExpirationTime < (currentTime - ALLOWED_CLOCK_SKEW);
        } catch (JwtTokenException e) {
            logError("Error while checking token expiration", e);
            return true;
        }
    }
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (JwtTokenParseException e) {
            logError("Failed to extract expiration from the token", e);
            throw new JwtTokenException("Failed to extract expiration from the token: " + e.getMessage(), e);
        }
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    public Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (claims.getExpiration().before(new Date())) {
                throw new JwtTokenExpiredException("JWT token has expired.");
            }
            return claims;
        } catch (JwtException e) {
            throw new JwtTokenExpiredException("JWT token has expired: " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        if (Objects.isNull(expirationMs) || expirationMs <= 0) {
            expirationMs = 3600000L;

        }
        try {
            this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Error initializing JwtTokenUtil with the secret key: {}", e.getMessage(), e);
            this.secretKey = Keys.hmacShaKeyFor("defaultSecretKey".getBytes(StandardCharsets.UTF_8));
        }
    }
    public boolean isValidJwtFormat(String token) {
        return token.chars().filter(ch -> ch == '.').count() == 2;
    }
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtTokenParseException e) {
            logError("Failed to extract username from the token", e);
            throw new JwtTokenException("Failed to extract username from the token: " + e.getMessage(), e);
        }
    }

    private void logError(String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage(), e);
    }

}
