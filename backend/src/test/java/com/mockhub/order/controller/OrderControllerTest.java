package com.mockhub.order.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.service.CalendarService;
import com.mockhub.order.service.OrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CalendarService calendarService;

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

    @Test
    @DisplayName("POST /api/v1/orders/checkout - unauthenticated - returns 401")
    void checkout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "mock"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - concurrent duplicate key - returns winner's order with 201")
    void checkout_concurrentDuplicateKey_returnsWinnersOrderWith201() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        OrderDto winnersOrder = new OrderDto(99L, "MH-20260807-0001", "PENDING",
                new BigDecimal("75.00"), new BigDecimal("7.50"), new BigDecimal("82.50"),
                "mock", null, Instant.now(), List.of(), null, null);

        // The loser of a same-idempotency-key race dies on the unique index; the
        // controller must recover with the winner's order, not surface a 500
        when(orderService.checkout(any(User.class), any(), eq("retry-key-1")))
                .thenThrow(new DataIntegrityViolationException("orders_idempotency_key_idx"));
        when(orderService.findOrderForIdempotentRetry(any(User.class), eq("retry-key-1")))
                .thenReturn(Optional.of(winnersOrder));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .with(user(new SecurityUser(testUser)))
                        .header("Idempotency-Key", "retry-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "mock"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("MH-20260807-0001"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - constraint violation with no matching order - propagates as 500")
    void checkout_constraintViolationNoMatchingOrder_propagatesAs500() throws Exception {
        User testUser = createTestUser();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));

        when(orderService.checkout(any(User.class), any(), any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));
        when(orderService.findOrderForIdempotentRetry(any(User.class), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .with(user(new SecurityUser(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "mock"}
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/v1/orders - unauthenticated - returns 401")
    void listOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderNumber} - unauthenticated - returns 401")
    void getOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/MH-20260317-0001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderNumber}/calendar - unauthenticated - returns 401")
    void getCalendar_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/MH-20260317-0001/calendar"))
                .andExpect(status().isUnauthorized());
    }
}
