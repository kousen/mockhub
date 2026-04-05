package com.mockhub.ticket.controller;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.mockhub.config.SecurityConfig;
import com.mockhub.ticket.dto.EarningsSummaryDto;
import com.mockhub.ticket.dto.OwnedTicketDto;
import com.mockhub.ticket.dto.SaleDto;
import com.mockhub.ticket.dto.SellerListingDto;
import com.mockhub.ticket.service.ListingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("seller@example.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Jane");
        user.setLastName("Seller");
        user.setEnabled(true);
        securityUser = new SecurityUser(user);
    }

    private void authenticateAs(SecurityUser principal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -- POST /api/v1/listings (unauthenticated) --

    @Test
    @DisplayName("POST /api/v1/listings - unauthenticated - returns 401")
    void createSellerListing_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventSlug": "test-concert",
                                    "sectionName": "Floor",
                                    "rowLabel": "A",
                                    "seatNumber": "1",
                                    "price": 75.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -- POST /api/v1/listings (authenticated) --

    @Test
    @DisplayName("POST /api/v1/listings - authenticated - returns 201 with listing")
    void createSellerListing_authenticated_returns201() throws Exception {
        authenticateAs(securityUser);

        SellerListingDto dto = new SellerListingDto(
                1L, 1L, "test-concert", "Test Concert",
                Instant.parse("2026-06-15T20:00:00Z"),
                "Madison Square Garden",
                "Floor", "A", "1", "RESERVED",
                new BigDecimal("75.00"), new BigDecimal("75.00"),
                new BigDecimal("50.00"), "ACTIVE", Instant.now(), Instant.now());

        when(listingService.createSellerListing(
                eq("seller@example.com"), any()))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventSlug": "test-concert",
                                    "sectionName": "Floor",
                                    "rowLabel": "A",
                                    "seatNumber": "1",
                                    "price": 75.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventSlug").value("test-concert"))
                .andExpect(jsonPath("$.sectionName").value("Floor"));

        verify(listingService).createSellerListing(
                eq("seller@example.com"), any());
    }

    // -- GET /api/v1/my/listings (unauthenticated) --

    @Test
    @DisplayName("GET /api/v1/my/listings - unauthenticated - returns 401")
    void getSellerListings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/my/listings"))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/v1/my/listings (authenticated) --

    @Test
    @DisplayName("GET /api/v1/my/listings - authenticated - returns 200 with listings")
    void getSellerListings_authenticated_returns200() throws Exception {
        authenticateAs(securityUser);

        SellerListingDto dto = new SellerListingDto(
                1L, 1L, "test-concert", "Test Concert",
                Instant.parse("2026-06-15T20:00:00Z"),
                "Madison Square Garden",
                "Floor", "A", "1", "RESERVED",
                new BigDecimal("75.00"), new BigDecimal("75.00"),
                new BigDecimal("50.00"), "ACTIVE", Instant.now(), Instant.now());

        when(listingService.getSellerListings("seller@example.com", null))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/my/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventSlug").value("test-concert"));
    }

    // -- PUT /api/v1/listings/{id}/price (unauthenticated) --

    @Test
    @DisplayName("PUT /api/v1/listings/{id}/price - unauthenticated - returns 401")
    void updateListingPrice_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/listings/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 100.00}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -- PUT /api/v1/listings/{id}/price (authenticated) --

    @Test
    @DisplayName("PUT /api/v1/listings/{id}/price - authenticated - returns 200")
    void updateListingPrice_authenticated_returns200() throws Exception {
        authenticateAs(securityUser);

        SellerListingDto dto = new SellerListingDto(
                1L, 1L, "test-concert", "Test Concert",
                Instant.parse("2026-06-15T20:00:00Z"),
                "Madison Square Garden",
                "Floor", "A", "1", "RESERVED",
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("50.00"), "ACTIVE", Instant.now(), Instant.now());

        when(listingService.updateListingPrice(eq(1L),
                eq("seller@example.com"), any()))
                .thenReturn(dto);

        mockMvc.perform(put("/api/v1/listings/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listedPrice").value(100.00));
    }

    // -- DELETE /api/v1/listings/{id} (unauthenticated) --

    @Test
    @DisplayName("DELETE /api/v1/listings/{id} - unauthenticated - returns 401")
    void deactivateListing_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/listings/1"))
                .andExpect(status().isUnauthorized());
    }

    // -- DELETE /api/v1/listings/{id} (authenticated) --

    @Test
    @DisplayName("DELETE /api/v1/listings/{id} - authenticated - returns 204")
    void deactivateListing_authenticated_returns204() throws Exception {
        authenticateAs(securityUser);

        mockMvc.perform(delete("/api/v1/listings/1"))
                .andExpect(status().isNoContent());

        verify(listingService).deactivateListing(1L, "seller@example.com");
    }

    // -- GET /api/v1/my/owned-tickets (unauthenticated) --

    @Test
    @DisplayName("GET /api/v1/my/owned-tickets - unauthenticated - returns 401")
    void getOwnedTickets_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/my/owned-tickets"))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/v1/my/owned-tickets (authenticated) --

    @Test
    @DisplayName("GET /api/v1/my/owned-tickets - authenticated - returns 200 with tickets")
    void getOwnedTickets_authenticated_returns200() throws Exception {
        authenticateAs(securityUser);

        OwnedTicketDto dto = new OwnedTicketDto(
                1L, "test-concert", "Test Concert",
                Instant.parse("2026-06-15T20:00:00Z"),
                "Madison Square Garden",
                "Floor", "A", "1", "RESERVED",
                new BigDecimal("50.00"));

        when(listingService.getOwnedTickets("seller@example.com"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/my/owned-tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(1))
                .andExpect(jsonPath("$[0].eventSlug").value("test-concert"))
                .andExpect(jsonPath("$[0].sectionName").value("Floor"))
                .andExpect(jsonPath("$[0].rowLabel").value("A"))
                .andExpect(jsonPath("$[0].seatNumber").value("1"));

        verify(listingService).getOwnedTickets("seller@example.com");
    }

    @Test
    @DisplayName("GET /api/v1/my/owned-tickets - authenticated with no tickets - returns empty list")
    void getOwnedTickets_authenticated_noTickets_returnsEmptyList() throws Exception {
        authenticateAs(securityUser);

        when(listingService.getOwnedTickets("seller@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/my/owned-tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -- GET /api/v1/my/earnings (unauthenticated) --

    @Test
    @DisplayName("GET /api/v1/my/earnings - unauthenticated - returns 401")
    void getEarningsSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/my/earnings"))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/v1/my/earnings (authenticated) --

    @Test
    @DisplayName("GET /api/v1/my/earnings - authenticated - returns 200")
    void getEarningsSummary_authenticated_returns200() throws Exception {
        authenticateAs(securityUser);

        EarningsSummaryDto summary = new EarningsSummaryDto(
                new BigDecimal("150.00"), 5L, 2L, 3L,
                List.of(new SaleDto(
                        "MH-20260320-0001", "Test Concert",
                        "Floor", "Floor, Row A, Seat 1",
                        new BigDecimal("75.00"), Instant.now())));

        when(listingService.getEarningsSummary("seller@example.com"))
                .thenReturn(summary);

        mockMvc.perform(get("/api/v1/my/earnings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnings").value(150.00))
                .andExpect(jsonPath("$.activeListings").value(2))
                .andExpect(jsonPath("$.recentSales[0].orderNumber")
                        .value("MH-20260320-0001"));
    }
}
