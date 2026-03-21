package com.mockhub.ticket.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.service.TicketPdfService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketDownloadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TicketDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketPdfService ticketPdfService;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/v1/orders/{orderNumber}/tickets/{ticketId}/download - unauthenticated - returns 401")
    void downloadTicket_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/MH-20260317-0001/tickets/1/download"))
                .andExpect(status().isUnauthorized());
    }
}
