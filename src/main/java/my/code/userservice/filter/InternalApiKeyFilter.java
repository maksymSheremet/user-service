package my.code.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.userservice.config.InternalApiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final InternalApiProperties internalApiProperties;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(internalApiProperties.getPathPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            log.warn("Internal API request without {} header: path={}",
                    API_KEY_HEADER, request.getRequestURI());
            sendUnauthorized(response, "Missing " + API_KEY_HEADER + " header");
            return;
        }

        if (!isValidKey(providedKey)) {
            log.warn("Internal API request with invalid key: path={}", request.getRequestURI());
            sendUnauthorized(response, "Invalid " + API_KEY_HEADER);
            return;
        }

        log.debug("Internal API request authorized: path={}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private boolean isValidKey(String providedKey) {
        byte[] expectedBytes = internalApiProperties.getKey().getBytes();
        byte[] providedBytes = providedKey.getBytes();
        return java.security.MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {
                  "status": 401,
                  "title": "Unauthorized",
                  "detail": "%s"
                }
                """.formatted(message));
    }
}
