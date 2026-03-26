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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Converts Spring AI's "Session not found" JSON-RPC errors to HTTP 404 responses.
 *
 * Spring AI 2.0.0-M3 returns session-not-found errors as HTTP 200 with a JSON-RPC
 * error body. MCP clients (like mcp-remote) can't recover from this because they
 * see a successful HTTP response with an opaque error payload.
 *
 * Per the MCP spec, an unknown session ID should return HTTP 404, which signals
 * the client to discard the stale session and re-initialize. This filter wraps
 * the response and converts session-not-found errors to proper 404 responses.
 *
 * This is needed because Railway (and similar platforms) redeploy containers,
 * wiping Spring AI's in-memory session store while clients still hold old
 * session IDs.
 */
@Component
@ConditionalOnProperty(name = "mockhub.mcp.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class McpSessionRecoveryFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(McpSessionRecoveryFilter.class);
    private static final String MCP_PATH_PREFIX = "/mcp/";
    private static final String SESSION_NOT_FOUND = "Session not found";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (!requestPath.startsWith(MCP_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);

        byte[] body = wrappedResponse.getContentAsByteArray();
        if (body.length > 0 && wrappedResponse.getStatus() == 200) {
            String responseBody = new String(body, wrappedResponse.getCharacterEncoding());
            if (responseBody.contains(SESSION_NOT_FOUND)) {
                log.warn("MCP session not found — returning 404 to trigger client re-initialization");
                wrappedResponse.resetBuffer();
                wrappedResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
                wrappedResponse.setContentType("application/json");
                wrappedResponse.getWriter().write(
                        "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600,\"message\":\"Session expired. Please reconnect.\"}}");
                wrappedResponse.copyBodyToResponse();
                return;
            }
        }

        wrappedResponse.copyBodyToResponse();
    }
}
