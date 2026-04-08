package com.mockhub.mandate.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.config.SecurityConfig;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.service.MandateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MandateController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MandateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandateService mandateService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        securityUser = new SecurityUser(user);
    }

    private void authenticateAs(SecurityUser principal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MandateDto sampleMandateDto() {
        return new MandateDto(
                1L,
                "test-mandate-123",
                "claude-desktop",
                "user@example.com",
                "PURCHASE",
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                "jazz,classical",
                null,
                "ACTIVE",
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now()
        );
    }

    // -- POST /api/v1/my/mandates (unauthenticated) --

    @Test
    @DisplayName("POST /api/v1/my/mandates - unauthenticated - returns 401")
    void createMandate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/my/mandates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "agentId": "claude-desktop",
                                    "scope": "PURCHASE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -- POST /api/v1/my/mandates (authenticated, real frontend payload without userEmail) --

    @Test
    @DisplayName("POST /api/v1/my/mandates - authenticated - returns 201 with authenticated user's email")
    void createMandate_authenticated_returns201() throws Exception {
        authenticateAs(securityUser);

        MandateDto dto = sampleMandateDto();
        when(mandateService.createMandate(argThat(req ->
                "user@example.com".equals(req.userEmail()))))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/my/mandates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "agentId": "claude-desktop",
                                    "scope": "PURCHASE",
                                    "maxSpendPerTransaction": 200.00,
                                    "maxSpendTotal": 1000.00,
                                    "allowedCategories": "jazz,classical"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mandateId").value("test-mandate-123"))
                .andExpect(jsonPath("$.agentId").value("claude-desktop"))
                .andExpect(jsonPath("$.userEmail").value("user@example.com"));

        verify(mandateService).createMandate(argThat(req ->
                "user@example.com".equals(req.userEmail())));
    }

    // -- POST /api/v1/my/mandates (missing agentId) --

    @Test
    @DisplayName("POST /api/v1/my/mandates - missing agentId - returns 400")
    void createMandate_missingAgentId_returns400() throws Exception {
        authenticateAs(securityUser);

        mockMvc.perform(post("/api/v1/my/mandates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "agentId": "",
                                    "scope": "PURCHASE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // -- GET /api/v1/my/mandates (unauthenticated) --

    @Test
    @DisplayName("GET /api/v1/my/mandates - unauthenticated - returns 401")
    void listMandates_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/my/mandates"))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/v1/my/mandates (authenticated) --

    @Test
    @DisplayName("GET /api/v1/my/mandates - authenticated - returns 200 with mandates")
    void listMandates_authenticated_returns200() throws Exception {
        authenticateAs(securityUser);

        MandateDto dto = sampleMandateDto();
        when(mandateService.listAllMandates("user@example.com"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/my/mandates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mandateId").value("test-mandate-123"))
                .andExpect(jsonPath("$[0].agentId").value("claude-desktop"))
                .andExpect(jsonPath("$[0].scope").value("PURCHASE"));
    }

    // -- GET /api/v1/my/mandates (empty list) --

    @Test
    @DisplayName("GET /api/v1/my/mandates - no mandates - returns empty array")
    void listMandates_emptyList_returns200() throws Exception {
        authenticateAs(securityUser);

        when(mandateService.listAllMandates("user@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/my/mandates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -- DELETE /api/v1/my/mandates/{mandateId} (unauthenticated) --

    @Test
    @DisplayName("DELETE /api/v1/my/mandates/{mandateId} - unauthenticated - returns 401")
    void revokeMandate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/my/mandates/test-mandate-123"))
                .andExpect(status().isUnauthorized());
    }

    // -- DELETE /api/v1/my/mandates/{mandateId} (authenticated) --

    @Test
    @DisplayName("DELETE /api/v1/my/mandates/{mandateId} - authenticated - returns 204")
    void revokeMandate_authenticated_returns204() throws Exception {
        authenticateAs(securityUser);

        doNothing().when(mandateService)
                .revokeMandate("test-mandate-123", "user@example.com");

        mockMvc.perform(delete("/api/v1/my/mandates/test-mandate-123"))
                .andExpect(status().isNoContent());

        verify(mandateService).revokeMandate("test-mandate-123", "user@example.com");
    }

    // -- DELETE /api/v1/my/mandates/{mandateId} (not found) --

    @Test
    @DisplayName("DELETE /api/v1/my/mandates/{mandateId} - not found - returns 404")
    void revokeMandate_notFound_returns404() throws Exception {
        authenticateAs(securityUser);

        doThrow(new ResourceNotFoundException("Mandate", "mandateId", "unknown-mandate"))
                .when(mandateService)
                .revokeMandate("unknown-mandate", "user@example.com");

        mockMvc.perform(delete("/api/v1/my/mandates/unknown-mandate"))
                .andExpect(status().isNotFound());
    }

    // -- DELETE /api/v1/my/mandates/{mandateId} (not owner) --

    @Test
    @DisplayName("DELETE /api/v1/my/mandates/{mandateId} - not owner - returns 401")
    void revokeMandate_notOwner_returns401() throws Exception {
        authenticateAs(securityUser);

        doThrow(new UnauthorizedException("You do not own this mandate"))
                .when(mandateService)
                .revokeMandate("other-mandate", "user@example.com");

        mockMvc.perform(delete("/api/v1/my/mandates/other-mandate"))
                .andExpect(status().isUnauthorized());
    }
}
