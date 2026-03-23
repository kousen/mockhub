package com.mockhub.acp;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AcpApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AcpApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ACP_PATH_PREFIX = "/acp/";

    private final String apiKey;

    public AcpApiKeyFilter(@Value("${mockhub.mcp.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (!requestPath.startsWith(ACP_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (apiKey.isBlank()) {
            log.warn("ACP API key is not configured, rejecting request to {}", requestPath);
            sendUnauthorized(response, "ACP API key is not configured");
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            log.warn("ACP request to {} missing API key", requestPath);
            sendUnauthorized(response, "Missing API key header: " + API_KEY_HEADER);
            return;
        }

        if (!apiKey.equals(providedKey.strip())) {
            log.warn("ACP request to {} with invalid API key", requestPath);
            sendUnauthorized(response, "Invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
