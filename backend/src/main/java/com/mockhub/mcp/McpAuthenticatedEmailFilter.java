package com.mockhub.mcp;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mockhub.ai.service.ChatContext;
import com.mockhub.auth.repository.UserRepository;

/**
 * Binds the authenticated OAuth2 token subject to {@link ChatContext} for MCP tool calls.
 *
 * <p>Active only on the {@code mcp-oauth2} resource server chain (see
 * {@link com.mockhub.mcp.config.McpOAuth2SecurityConfig}). When a user authorizes the MCP
 * connector through the OAuth 2.1 authorization-code flow, the access token's subject is the
 * user's email. This filter resolves that subject to a known MockHub user and pins it into
 * {@link ChatContext}, so the MCP tools act strictly as the authenticated user regardless of any
 * {@code userEmail} parameter the agent supplies.</p>
 *
 * <p>This closes the impersonation gap that would otherwise let any token holder act as — or mint
 * mandates and payment credentials for — a different user simply by passing that user's email. It
 * mirrors the website-chat enforcement, where {@code ChatService} pins the authenticated email
 * before invoking the ChatClient tools.</p>
 *
 * <p>If the subject is not a known user — for example a client-credentials token, which carries no
 * user context, or an OAuth client identifier — the context is left unset and the tool parameter is
 * honored as-is. Under plain {@code X-API-Key} auth this filter is absent entirely, so email remains
 * self-asserted (the trusted-operator model).</p>
 */
public class McpAuthenticatedEmailFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public McpAuthenticatedEmailFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            bindAuthenticatedEmail();
            filterChain.doFilter(request, response);
        } finally {
            ChatContext.clear();
        }
    }

    private void bindAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        String subject = authentication.getName();
        if (subject == null || subject.isBlank()) {
            return;
        }
        userRepository.findByEmail(subject.strip())
                .ifPresent(user -> ChatContext.setAuthenticatedEmail(user.getEmail()));
    }
}
