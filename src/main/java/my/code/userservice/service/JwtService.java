package my.code.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public Long extractUserId(String token) {
        try {
            Claims claims = parseClaims(token);

            String tokenType = claims.get("tokenType", String.class);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("Invalid token type: expected ACCESS, got '{}'", tokenType);
                return null;
            }

            return claims.get("userId", Long.class);

        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Unexpected error during JWT parsing", ex);
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}
