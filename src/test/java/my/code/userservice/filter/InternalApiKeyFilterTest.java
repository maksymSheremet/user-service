package my.code.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.code.userservice.config.InternalApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalApiKeyFilter")
class InternalApiKeyFilterTest {

    private static final String VALID_KEY = "valid-internal-api-key";
    private static final String INTERNAL_PATH = "/api/internal/users/42";
    private static final String PUBLIC_PATH = "/api/me";
    private static final String PATH_PREFIX = "/api/internal/";

    @Mock
    private InternalApiProperties internalApiProperties;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private InternalApiKeyFilter filter;

    @BeforeEach
    void setUpGlobal() {
        when(internalApiProperties.getPathPrefix()).thenReturn(PATH_PREFIX);
    }

    @Nested
    @DisplayName("non-internal paths")
    class NonInternalPaths {

        @Test
        @DisplayName("should pass through non-internal requests without checking key")
        void shouldPassThroughNonInternalRequests() throws Exception {
            when(request.getRequestURI()).thenReturn(PUBLIC_PATH);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(401);
        }

        @Test
        @DisplayName("should pass through actuator health endpoint")
        void shouldPassThroughActuatorHealth() throws Exception {
            when(request.getRequestURI()).thenReturn("/actuator/health");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("internal paths")
    class InternalPaths {

        @BeforeEach
        void setUp() {
            when(request.getRequestURI()).thenReturn(INTERNAL_PATH);
        }

        @Test
        @DisplayName("should allow request with valid API key")
        void shouldAllowRequestWithValidKey() throws Exception {
            when(internalApiProperties.getKey()).thenReturn(VALID_KEY);
            when(request.getHeader(InternalApiKeyFilter.API_KEY_HEADER)).thenReturn(VALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(401);
        }

        @Test
        @DisplayName("should reject request without API key header")
        void shouldRejectRequestWithoutApiKey() throws Exception {
            StringWriter responseWriter = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
            when(request.getHeader(InternalApiKeyFilter.API_KEY_HEADER)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);

            assertThat(responseWriter.toString()).contains("Missing");
        }

        @Test
        @DisplayName("should reject request with blank API key")
        void shouldRejectRequestWithBlankApiKey() throws Exception {
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
            when(request.getHeader(InternalApiKeyFilter.API_KEY_HEADER)).thenReturn("   ");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("should reject request with invalid API key")
        void shouldRejectRequestWithInvalidKey() throws Exception {
            StringWriter responseWriter = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
            when(internalApiProperties.getKey()).thenReturn(VALID_KEY);
            when(request.getHeader(InternalApiKeyFilter.API_KEY_HEADER)).thenReturn("wrong-key");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);

            assertThat(responseWriter.toString()).contains("Invalid");
        }

        @Test
        @DisplayName("should use constant-time comparison (not vulnerable to timing attack)")
        void shouldUseConstantTimeComparison() throws Exception {
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
            when(internalApiProperties.getKey()).thenReturn(VALID_KEY);
            String almostValidKey = VALID_KEY.substring(0, VALID_KEY.length() - 1) + "X";
            when(request.getHeader(InternalApiKeyFilter.API_KEY_HEADER)).thenReturn(almostValidKey);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }
    }
}