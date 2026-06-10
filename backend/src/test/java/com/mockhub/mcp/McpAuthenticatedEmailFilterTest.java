package com.mockhub.mcp;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockhub.ai.service.ChatContext;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpAuthenticatedEmailFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private McpAuthenticatedEmailFilter filter;

    @BeforeEach
    void setUp() {
        filter = new McpAuthenticatedEmailFilter(userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ChatContext.clear();
    }

    @Test
    @DisplayName("doFilterInternal - given authenticated known user - pins email for the request and clears after")
    void doFilterInternal_givenAuthenticatedKnownUser_pinsEmailThenClears() throws Exception {
        authenticateAs("buyer@test.com");
        User user = new User();
        user.setEmail("buyer@test.com");
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        AtomicReference<String> emailDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> emailDuringChain.set(ChatContext.getAuthenticatedEmail());

        filter.doFilterInternal(request, response, chain);

        assertEquals("buyer@test.com", emailDuringChain.get(),
                "Tool calls should see the authenticated user's email");
        assertNull(ChatContext.getAuthenticatedEmail(), "Context must be cleared after the request");
    }

    @Test
    @DisplayName("doFilterInternal - given subject is not a known user - leaves context unset")
    void doFilterInternal_givenUnknownSubject_leavesContextUnset() throws Exception {
        authenticateAs("claude-mcp-client");
        when(userRepository.findByEmail("claude-mcp-client")).thenReturn(Optional.empty());

        AtomicReference<String> emailDuringChain = new AtomicReference<>("sentinel");
        FilterChain chain = (req, res) -> emailDuringChain.set(ChatContext.getAuthenticatedEmail());

        filter.doFilterInternal(request, response, chain);

        assertNull(emailDuringChain.get(),
                "Client-credentials / non-user subjects must not be pinned as a user");
    }

    @Test
    @DisplayName("doFilterInternal - given no authentication - does not pin email and does not query users")
    void doFilterInternal_givenNoAuthentication_doesNothing() throws Exception {
        AtomicReference<String> emailDuringChain = new AtomicReference<>("sentinel");
        FilterChain chain = (req, res) -> emailDuringChain.set(ChatContext.getAuthenticatedEmail());

        filter.doFilterInternal(request, response, chain);

        assertNull(emailDuringChain.get(), "Unauthenticated requests must not pin any email");
    }

    @Test
    @DisplayName("shouldNotFilter - skips non-MCP paths, applies to /mcp paths")
    void shouldNotFilter_skipsNonMcpPaths() {
        when(request.getRequestURI()).thenReturn("/api/v1/events");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/mcp/message");
        assertFalse(filter.shouldNotFilter(request));
    }

    private void authenticateAs(String subject) {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(subject, "credentials", "ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
