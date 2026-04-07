package com.mockhub.mcp;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Logs every request to /mcp/** with method, path, auth status, client info,
 * and response status. Used to diagnose MCP connection failures where
 * initialize succeeds but tools/list never arrives.
 */
@Component
@ConditionalOnProperty(name = "mockhub.mcp.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class McpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(McpRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String sessionId = request.getHeader("Mcp-Session-Id");
        String userAgent = request.getHeader("User-Agent");
        String auth = request.getHeader("Authorization");
        String authSummary = auth == null ? "none"
                : auth.startsWith("Bearer ") ? "Bearer (len=" + auth.length() + ")"
                : "other";

        // For POST requests, capture the request body to see the JSON-RPC method
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 4096);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        log.info("MCP request: {} {} | session={} | auth={} | ua={}",
                method, path, sessionId, authSummary, truncate(userAgent, 60));

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        int status = wrappedResponse.getStatus();

        // Extract JSON-RPC method from request body if available
        byte[] requestBody = wrappedRequest.getContentAsByteArray();
        String jsonRpcMethod = extractJsonRpcMethod(requestBody, wrappedRequest.getCharacterEncoding());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : "anonymous";

        log.info("MCP response: {} {} → {} | method={} | principal={} | session={}",
                method, path, status, jsonRpcMethod, principal, sessionId);

        wrappedResponse.copyBodyToResponse();
    }

    private String extractJsonRpcMethod(byte[] body, String encoding) {
        if (body == null || body.length == 0) {
            return "n/a";
        }
        try {
            String bodyStr = new String(body, encoding);
            int methodIdx = bodyStr.indexOf("\"method\"");
            if (methodIdx < 0) {
                return "n/a";
            }
            int colonIdx = bodyStr.indexOf(':', methodIdx);
            int quoteStart = bodyStr.indexOf('"', colonIdx + 1);
            int quoteEnd = bodyStr.indexOf('"', quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                return bodyStr.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception ex) {
            // Don't let logging failures break requests
        }
        return "parse-error";
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "null";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...";
    }
}
