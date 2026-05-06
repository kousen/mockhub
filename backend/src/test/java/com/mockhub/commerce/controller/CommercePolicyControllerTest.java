package com.mockhub.commerce.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.commerce.dto.CommercePolicyDto;
import com.mockhub.commerce.dto.CommercePolicySectionDto;
import com.mockhub.commerce.service.CommercePolicyService;
import com.mockhub.config.SecurityConfig;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommercePolicyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CommercePolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommercePolicyService commercePolicyService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/v1/commerce/policies/default - returns public default policy")
    void getDefaultPolicy_returnsPublicDefaultPolicy() throws Exception {
        when(commercePolicyService.getDefaultPolicy()).thenReturn(createPolicy(null));

        mockMvc.perform(get("/api/v1/commerce/policies/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("policy"))
                .andExpect(jsonPath("$.appliesTo").value("all_mockhub_ticket_purchases"))
                .andExpect(jsonPath("$.sections[0].code").value("refunds"));
    }

    @Test
    @DisplayName("GET /api/v1/commerce/policies/events/{eventSlug} - returns event policy")
    void getEventPolicy_returnsEventPolicy() throws Exception {
        when(commercePolicyService.getPolicyForEvent("rock-festival")).thenReturn(createPolicy("rock-festival"));

        mockMvc.perform(get("/api/v1/commerce/policies/events/rock-festival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventSlug").value("rock-festival"))
                .andExpect(jsonPath("$.policyUrl").value("/api/v1/commerce/policies/events/rock-festival"));
    }

    private CommercePolicyDto createPolicy(String eventSlug) {
        return new CommercePolicyDto(
                "policy",
                "v1",
                eventSlug == null ? "all_mockhub_ticket_purchases" : "event:" + eventSlug,
                eventSlug,
                List.of(new CommercePolicySectionDto(
                        "refunds", "Refund policy", "summary", "details", "guidance")),
                "support@mockhub.dev",
                "/support",
                eventSlug == null ? "/api/v1/commerce/policies/default"
                        : "/api/v1/commerce/policies/events/" + eventSlug,
                Instant.now());
    }
}
