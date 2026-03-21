package com.mockhub.ticket.controller;

import java.time.Instant;
import java.util.Optional;

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
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.service.TicketSigningService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketVerificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TicketVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketSigningService ticketSigningService;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Claims createMockClaims() {
        return Jwts.claims().subject("MH-20260321-0001")
                .add("tic", 456L)
                .add("evt", "rock-festival-2026")
                .add("sec", "Floor A")
                .add("row", "12")
                .add("seat", "7")
                .build();
    }

    @Test
    @DisplayName("verifyTicket - given valid token - returns valid result")
    void verifyTicket_givenValidToken_returnsValidResult() throws Exception {
        Claims claims = createMockClaims();
        OrderItem orderItem = new OrderItem();
        orderItem.setScannedAt(null);

        when(ticketSigningService.verifyToken("valid-token")).thenReturn(claims);
        when(orderItemRepository.findByOrderNumberAndTicketId("MH-20260321-0001", 456L))
                .thenReturn(Optional.of(orderItem));

        mockMvc.perform(get("/api/v1/tickets/verify").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.orderNumber").value("MH-20260321-0001"))
                .andExpect(jsonPath("$.ticketId").value(456))
                .andExpect(jsonPath("$.eventSlug").value("rock-festival-2026"))
                .andExpect(jsonPath("$.sectionName").value("Floor A"))
                .andExpect(jsonPath("$.rowLabel").value("12"))
                .andExpect(jsonPath("$.seatNumber").value("7"))
                .andExpect(jsonPath("$.alreadyScanned").value(false))
                .andExpect(jsonPath("$.message").value("Ticket verified successfully"));
    }

    @Test
    @DisplayName("verifyTicket - given invalid token - returns invalid result")
    void verifyTicket_givenInvalidToken_returnsInvalidResult() throws Exception {
        when(ticketSigningService.verifyToken("bad-token"))
                .thenThrow(new JwtException("bad token"));

        mockMvc.perform(get("/api/v1/tickets/verify").param("token", "bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.orderNumber").doesNotExist())
                .andExpect(jsonPath("$.ticketId").doesNotExist())
                .andExpect(jsonPath("$.alreadyScanned").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or tampered ticket"));
    }

    @Test
    @DisplayName("verifyTicket - given already scanned ticket - returns already scanned warning")
    void verifyTicket_givenAlreadyScannedTicket_returnsAlreadyScannedWarning() throws Exception {
        Claims claims = createMockClaims();
        Instant scannedTime = Instant.parse("2026-03-21T14:30:00Z");
        OrderItem orderItem = new OrderItem();
        orderItem.setScannedAt(scannedTime);

        when(ticketSigningService.verifyToken("scanned-token")).thenReturn(claims);
        when(orderItemRepository.findByOrderNumberAndTicketId("MH-20260321-0001", 456L))
                .thenReturn(Optional.of(orderItem));

        mockMvc.perform(get("/api/v1/tickets/verify").param("token", "scanned-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.alreadyScanned").value(true))
                .andExpect(jsonPath("$.scannedAt").value("2026-03-21T14:30:00Z"))
                .andExpect(jsonPath("$.message").value("Ticket already scanned at 2026-03-21T14:30:00Z"));
    }

    @Test
    @DisplayName("verifyTicket - given no token param - returns error")
    void verifyTicket_givenNoTokenParam_returnsError() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/verify"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("verifyTicket - is public endpoint - returns 200 without auth")
    void verifyTicket_isPublicEndpoint_returns200WithoutAuth() throws Exception {
        Claims claims = createMockClaims();
        OrderItem orderItem = new OrderItem();
        orderItem.setScannedAt(null);

        when(ticketSigningService.verifyToken("public-token")).thenReturn(claims);
        when(orderItemRepository.findByOrderNumberAndTicketId("MH-20260321-0001", 456L))
                .thenReturn(Optional.of(orderItem));

        mockMvc.perform(get("/api/v1/tickets/verify").param("token", "public-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }
}
