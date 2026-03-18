package com.mockhub.notification.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.config.SecurityConfig;
import com.mockhub.notification.dto.NotificationDto;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private User createTestUser() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setPasswordHash("hash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRoles(Set.of(buyerRole));
        return testUser;
    }

    private SecurityUser createSecurityUser() {
        return new SecurityUser(createTestUser());
    }

    private NotificationDto createTestNotification(Long id) {
        return new NotificationDto(
                id, NotificationType.ORDER_CONFIRMED,
                "Order Confirmed", "Your order has been confirmed",
                "/orders/ORD-123", false, Instant.now());
    }

    @Test
    @DisplayName("GET /api/v1/notifications - unauthenticated - returns 401")
    void listNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notifications - authenticated - returns paged notifications")
    void listNotifications_authenticated_returnsPagedNotifications() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        PagedResponse<NotificationDto> pagedResponse = new PagedResponse<>(
                List.of(createTestNotification(1L), createTestNotification(2L)),
                0, 20, 2, 1);
        when(notificationService.getUserNotifications(any(User.class), eq(0), eq(20)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/notifications")
                        .with(user(createSecurityUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Order Confirmed"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/notifications - with pagination params - passes params to service")
    void listNotifications_withPaginationParams_passesParamsToService() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        PagedResponse<NotificationDto> pagedResponse = new PagedResponse<>(
                List.of(), 2, 10, 0, 0);
        when(notificationService.getUserNotifications(any(User.class), eq(2), eq(10)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "2")
                        .param("size", "10")
                        .with(user(createSecurityUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count - unauthenticated - returns 401")
    void getUnreadCount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count - authenticated - returns count")
    void getUnreadCount_authenticated_returnsCount() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(notificationService.getUnreadCount(any(User.class))).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(user(createSecurityUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read - unauthenticated - returns 401")
    void markAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/1/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read - authenticated - returns 204")
    void markAsRead_authenticated_returns204() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(put("/api/v1/notifications/1/read")
                        .with(user(createSecurityUser())))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(any(User.class), eq(1L));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/read-all - unauthenticated - returns 401")
    void markAllAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/read-all - authenticated - returns 204")
    void markAllAsRead_authenticated_returns204() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .with(user(createSecurityUser())))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(any(User.class));
    }
}
