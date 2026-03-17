package com.mockhub.order.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.order.service.OrderService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

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
}
