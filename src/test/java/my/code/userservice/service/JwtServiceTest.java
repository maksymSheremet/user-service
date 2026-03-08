package my.code.userservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import my.code.userservice.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";
    private static final Long USER_ID = 42L;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("should return userId from valid ACCESS token")
        void shouldReturnUserIdFromValidAccessToken() {
            String token = generateAccessToken();

            Long result = jwtService.extractUserId(token);

            assertThat(result).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should return null for REFRESH token (token substitution protection)")
        void shouldReturnNullForRefreshToken() {
            String refreshToken = generateRefreshToken();

            Long result = jwtService.extractUserId(refreshToken);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            String expiredToken = generateExpiredToken();

            Long result = jwtService.extractUserId(expiredToken);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for tampered token")
        void shouldReturnNullForTamperedToken() {
            String token = generateAccessToken();
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            Long result = jwtService.extractUserId(tamperedToken);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for token signed with wrong key")
        void shouldReturnNullForTokenSignedWithWrongKey() {
            String wrongSecret = "wrong-secret-key-that-is-long-enough-for-hmac-sha256!!";
            SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

            String tokenWithWrongKey = Jwts.builder()
                    .claim("userId", USER_ID)
                    .claim("tokenType", "ACCESS")
                    .expiration(new Date(System.currentTimeMillis() + 3600_000))
                    .signWith(wrongKey)
                    .compact();

            Long result = jwtService.extractUserId(tokenWithWrongKey);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for malformed token string")
        void shouldReturnNullForMalformedToken() {
            Long result = jwtService.extractUserId("not.a.valid.jwt.token");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for empty string")
        void shouldReturnNullForEmptyString() {
            Long result = jwtService.extractUserId("");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("should return role from JWT claim")
        void shouldReturnRoleFromClaim() {
            String token = generateTokenWithRole("ADMIN");
            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should return USER role from JWT claim")
        void shouldReturnUserRoleFromClaim() {
            String token = generateTokenWithRole("USER");
            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("USER");
        }

        @Test
        @DisplayName("should return default USER when role claim is missing")
        void shouldReturnDefaultRoleWhenClaimMissing() {
            String token = generateAccessToken();
            String role = jwtService.extractRole(token);

            assertThat(role).isEqualTo("USER");
        }

        @Test
        @DisplayName("should return default USER for invalid token")
        void shouldReturnDefaultRoleForInvalidToken() {
            String role = jwtService.extractRole("invalid.token.here");

            assertThat(role).isEqualTo("USER");
        }
    }

    private String generateAccessToken() {
        return generateToken("ACCESS", new Date(System.currentTimeMillis() + 3600_000));
    }

    private String generateRefreshToken() {
        return generateToken("REFRESH", new Date(System.currentTimeMillis() + 86400_000));
    }

    private String generateExpiredToken() {
        return generateToken("ACCESS", new Date(System.currentTimeMillis() - 1000));
    }

    private String generateToken(String tokenType, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("userId", USER_ID)
                .claim("tokenType", tokenType)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private String generateTokenWithRole(String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("userId", USER_ID)
                .claim("tokenType", "ACCESS")
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

}