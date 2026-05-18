package com.mockhub.buyerpreference.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.mockhub.buyerpreference.dto.BuyerPreferencesDto;
import com.mockhub.buyerpreference.entity.RiskTolerance;
import com.mockhub.buyerpreference.service.BuyerPreferenceService;
import com.mockhub.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerPreferenceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BuyerPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuyerPreferenceService buyerPreferenceService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(42L);
        user.setEmail("buyer@example.com");
        user.setFirstName("Buyer");
        user.setLastName("Example");
        securityUser = new SecurityUser(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/preferences/me - unauthenticated - returns 401")
    void getPreferences_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/preferences/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/preferences/me - authenticated - returns current preferences")
    void getPreferences_authenticated_returnsPreferences() throws Exception {
        authenticateAs(securityUser);
        when(buyerPreferenceService.getPreferences("buyer@example.com"))
                .thenReturn(samplePreferences());

        mockMvc.perform(get("/api/v1/preferences/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredArtists[0]").value("Radiohead"))
                .andExpect(jsonPath("$.preferredCities[0]").value("Boston"))
                .andExpect(jsonPath("$.riskTolerance").value("LOW_RISK_ONLY"));
    }

    @Test
    @DisplayName("PUT /api/v1/preferences/me - authenticated - updates current user's preferences")
    void updatePreferences_authenticated_updatesPreferences() throws Exception {
        authenticateAs(securityUser);
        when(buyerPreferenceService.updatePreferences(eq("buyer@example.com"), any()))
                .thenReturn(samplePreferences());

        mockMvc.perform(put("/api/v1/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "preferredArtists": ["Radiohead"],
                                    "preferredCities": ["Boston"],
                                    "preferredSections": ["Orchestra"],
                                    "maxTotalPrice": 150.00,
                                    "allInPriceOnly": true,
                                    "riskTolerance": "LOW_RISK_ONLY",
                                    "willingToWaitForPriceDrops": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTotalPrice").value(150.00))
                .andExpect(jsonPath("$.allInPriceOnly").value(true));

        verify(buyerPreferenceService).updatePreferences(eq("buyer@example.com"), any());
    }

    private void authenticateAs(SecurityUser principal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BuyerPreferencesDto samplePreferences() {
        Instant now = Instant.now();
        return new BuyerPreferencesDto(
                1L,
                42L,
                List.of("Radiohead"),
                List.of("concerts"),
                List.of("Boston"),
                List.of("Blue Note"),
                List.of("Orchestra"),
                List.of(),
                List.of(),
                new BigDecimal("150.00"),
                null,
                true,
                "aisle seating",
                RiskTolerance.LOW_RISK_ONLY,
                true,
                now,
                now);
    }
}
