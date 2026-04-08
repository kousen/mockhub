package com.mockhub.admin.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.admin.dto.DashboardStatsDto;
import com.mockhub.admin.dto.DemoResetResultDto;
import com.mockhub.admin.service.AdminDashboardService;
import com.mockhub.admin.service.AdminEventService;
import com.mockhub.admin.service.AdminOrderService;
import com.mockhub.admin.service.AdminUserService;
import com.mockhub.admin.service.DemoResetService;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.ticketmaster.service.TicketmasterSyncService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private AdminEventService adminEventService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private TicketmasterSyncService ticketmasterSyncService;

    @MockitoBean
    private DemoResetService demoResetService;

    @Test
    @DisplayName("GET /api/v1/admin/dashboard - unauthenticated - returns 401")
    void getDashboardStats_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard - non-admin user - returns 403")
    @WithMockUser(roles = "BUYER")
    void getDashboardStats_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard - admin user - returns 200 with stats")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getDashboardStats_admin_returns200WithStats() throws Exception {
        DashboardStatsDto stats = new DashboardStatsDto(
                100L, 50L, new BigDecimal("10000.00"), 25L, 500L, List.of());
        when(adminDashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalOrders").value(50))
                .andExpect(jsonPath("$.activeEvents").value(25));
    }

    @Test
    @DisplayName("GET /api/v1/admin/events - non-admin user - returns 403")
    @WithMockUser(roles = "BUYER")
    void listEvents_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - non-admin user - returns 403")
    @WithMockUser(roles = "BUYER")
    void listUsers_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders - unauthenticated - returns 401")
    void listOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/admin/ticketmaster/sync - admin - returns 200 and triggers sync")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void triggerTicketmasterSync_admin_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ticketmaster/sync"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("Sync triggered successfully"));

        verify(ticketmasterSyncService).syncEvents();
    }

    @Test
    @DisplayName("POST /api/v1/admin/ticketmaster/sync - unauthenticated - returns 401")
    void triggerTicketmasterSync_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ticketmaster/sync"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/admin/ticketmaster/sync - non-admin - returns 403")
    @WithMockUser(roles = "BUYER")
    void triggerTicketmasterSync_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ticketmaster/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/ticketmaster/activate - admin - returns 200 with counts")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void activateTicketmasterEvents_admin_returns200() throws Exception {
        when(adminEventService.activateTicketmasterEvents())
                .thenReturn(java.util.Map.of(
                        "seedEventsDeactivated", 100,
                        "ticketmasterEventsFeatured", 83,
                        "pastEventsCompleted", 5));

        mockMvc.perform(post("/api/v1/admin/ticketmaster/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seedEventsDeactivated").value(100))
                .andExpect(jsonPath("$.ticketmasterEventsFeatured").value(83));
    }

    // -- POST /api/v1/admin/demo/reset (unauthenticated) --

    @Test
    @DisplayName("POST /api/v1/admin/demo/reset - unauthenticated - returns 401")
    void resetDemoUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\": \"buyer@mockhub.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    // -- POST /api/v1/admin/demo/reset (non-admin) --

    @Test
    @DisplayName("POST /api/v1/admin/demo/reset - non-admin - returns 403")
    @WithMockUser(roles = "BUYER")
    void resetDemoUser_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\": \"buyer@mockhub.com\"}"))
                .andExpect(status().isForbidden());
    }

    // -- POST /api/v1/admin/demo/reset (admin, valid) --

    @Test
    @DisplayName("POST /api/v1/admin/demo/reset - admin - returns 200 with summary")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void resetDemoUser_admin_returns200WithSummary() throws Exception {
        DemoResetResultDto result = new DemoResetResultDto(
                "buyer@mockhub.com", true,
                List.of("MH-20260408-0001"), List.of("mandate-001"));

        when(demoResetService.resetUser("buyer@mockhub.com")).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\": \"buyer@mockhub.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("buyer@mockhub.com"))
                .andExpect(jsonPath("$.cartCleared").value(true))
                .andExpect(jsonPath("$.cancelledOrders[0]").value("MH-20260408-0001"))
                .andExpect(jsonPath("$.revokedMandates[0]").value("mandate-001"));

        verify(demoResetService).resetUser("buyer@mockhub.com");
    }

    // -- POST /api/v1/admin/demo/reset (missing email) --

    @Test
    @DisplayName("POST /api/v1/admin/demo/reset - missing email - returns 400")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void resetDemoUser_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    // -- POST /api/v1/admin/demo/reset (user not found) --

    @Test
    @DisplayName("POST /api/v1/admin/demo/reset - user not found - returns 404")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void resetDemoUser_userNotFound_returns404() throws Exception {
        when(demoResetService.resetUser("unknown@example.com"))
                .thenThrow(new ResourceNotFoundException("User", "email", "unknown@example.com"));

        mockMvc.perform(post("/api/v1/admin/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\": \"unknown@example.com\"}"))
                .andExpect(status().isNotFound());
    }
}
