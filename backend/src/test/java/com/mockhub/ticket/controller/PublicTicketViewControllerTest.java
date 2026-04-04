package com.mockhub.ticket.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.QrCodeService;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicTicketViewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PublicTicketViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketSigningService ticketSigningService;

    @MockitoBean
    private QrCodeService qrCodeService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Claims createOrderViewClaims(String orderNumber) {
        return Jwts.claims().subject(orderNumber)
                .add("typ", "order-view")
                .build();
    }

    private Order createTestOrder(String orderNumber) {
        Venue venue = new Venue();
        venue.setName("Carnegie Hall");
        venue.setCity("New York");
        venue.setState("NY");

        Event event = new Event();
        event.setName("Yo-Yo Ma - Bach Cello Suites");
        event.setSlug("yo-yo-ma-bach-cello-suites");
        event.setEventDate(Instant.parse("2026-06-15T23:00:00Z"));
        event.setVenue(venue);

        Section section = new Section();
        section.setName("Orchestra");

        SeatRow row = new SeatRow();
        row.setRowLabel("A");

        Seat seat = new Seat();
        seat.setSeatNumber("7");
        seat.setRow(row);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setSection(section);
        ticket.setSeat(seat);
        ticket.setTicketType("STANDARD");

        Listing listing = new Listing();
        listing.setEvent(event);

        User user = new User();
        user.setEmail("buyer@example.com");

        OrderItem item = new OrderItem();
        item.setTicket(ticket);
        item.setListing(listing);
        item.setPricePaid(new BigDecimal("103.09"));

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUser(user);
        order.setTotal(new BigDecimal("113.40"));
        order.setItems(List.of(item));
        item.setOrder(order);

        return order;
    }

    @Test
    @DisplayName("viewTickets - given valid token - returns order with ticket details")
    void viewTickets_givenValidToken_returnsOrderWithTickets() throws Exception {
        String orderNumber = "MH-20260322-0003";
        Claims claims = createOrderViewClaims(orderNumber);
        Order order = createTestOrder(orderNumber);

        when(ticketSigningService.verifyOrderViewToken("valid-token")).thenReturn(claims);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/tickets/view").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.eventName").value("Yo-Yo Ma - Bach Cello Suites"))
                .andExpect(jsonPath("$.venueName").value("Carnegie Hall"))
                .andExpect(jsonPath("$.venueLocation").value("New York, NY"))
                .andExpect(jsonPath("$.tickets").isArray())
                .andExpect(jsonPath("$.tickets[0].ticketId").value(1))
                .andExpect(jsonPath("$.tickets[0].sectionName").value("Orchestra"))
                .andExpect(jsonPath("$.tickets[0].rowLabel").value("A"))
                .andExpect(jsonPath("$.tickets[0].seatNumber").value("7"))
                .andExpect(jsonPath("$.tickets[0].ticketType").value("STANDARD"))
                .andExpect(jsonPath("$.tickets[0].qrCodeUrl").exists());
    }

    @Test
    @DisplayName("viewTickets - given invalid token - returns 400")
    void viewTickets_givenInvalidToken_returns400() throws Exception {
        when(ticketSigningService.verifyOrderViewToken("bad-token"))
                .thenThrow(new JwtException("not an order-view token"));

        mockMvc.perform(get("/api/v1/tickets/view").param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("viewTickets - given nonexistent order - returns 404")
    void viewTickets_givenNonexistentOrder_returns404() throws Exception {
        Claims claims = createOrderViewClaims("MH-NONEXISTENT");
        when(ticketSigningService.verifyOrderViewToken("orphan-token")).thenReturn(claims);
        when(orderRepository.findByOrderNumber("MH-NONEXISTENT")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tickets/view").param("token", "orphan-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("viewTickets - given pending order - returns 409")
    void viewTickets_givenPendingOrder_returns409() throws Exception {
        String orderNumber = "MH-20260322-0004";
        Claims claims = createOrderViewClaims(orderNumber);
        Order order = createTestOrder(orderNumber);
        order.setStatus(OrderStatus.PENDING);

        when(ticketSigningService.verifyOrderViewToken("pending-token")).thenReturn(claims);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/tickets/view").param("token", "pending-token"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("viewTickets - is public endpoint - returns 200 without auth")
    void viewTickets_isPublicEndpoint_returns200WithoutAuth() throws Exception {
        String orderNumber = "MH-20260322-0005";
        Claims claims = createOrderViewClaims(orderNumber);
        Order order = createTestOrder(orderNumber);

        when(ticketSigningService.verifyOrderViewToken("public-token")).thenReturn(claims);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/tickets/view").param("token", "public-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));
    }

    @Test
    @DisplayName("getQrCode - given valid token - returns PNG image")
    void getQrCode_givenValidToken_returnsPngImage() throws Exception {
        String orderNumber = "MH-20260322-0003";
        Claims claims = createOrderViewClaims(orderNumber);
        Order order = createTestOrder(orderNumber);
        OrderItem item = order.getItems().getFirst();

        when(ticketSigningService.verifyOrderViewToken("valid-token")).thenReturn(claims);
        when(orderItemRepository.findByOrderNumberAndTicketId(orderNumber, 1L))
                .thenReturn(Optional.of(item));
        when(ticketSigningService.generateToken(anyString(), anyLong(), anyString(),
                anyString(), anyString(), anyString())).thenReturn("ticket-jwt");
        when(qrCodeService.generateQrCode(anyString(), anyInt()))
                .thenReturn(new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        mockMvc.perform(get("/api/v1/tickets/{orderNumber}/{ticketId}/qr", orderNumber, 1L)
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    @DisplayName("getQrCode - given invalid token - returns 400")
    void getQrCode_givenInvalidToken_returns400() throws Exception {
        when(ticketSigningService.verifyOrderViewToken("bad-token"))
                .thenThrow(new JwtException("invalid"));

        mockMvc.perform(get("/api/v1/tickets/{orderNumber}/{ticketId}/qr",
                        "MH-20260322-0003", 1L)
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getQrCode - given mismatched order number - returns 400")
    void getQrCode_givenMismatchedOrderNumber_returns400() throws Exception {
        Claims claims = createOrderViewClaims("MH-DIFFERENT");
        when(ticketSigningService.verifyOrderViewToken("wrong-order-token")).thenReturn(claims);

        mockMvc.perform(get("/api/v1/tickets/{orderNumber}/{ticketId}/qr",
                        "MH-20260322-0003", 1L)
                        .param("token", "wrong-order-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getQrCode - given nonexistent ticket - returns 404")
    void getQrCode_givenNonexistentTicket_returns404() throws Exception {
        String orderNumber = "MH-20260322-0003";
        Claims claims = createOrderViewClaims(orderNumber);

        when(ticketSigningService.verifyOrderViewToken("valid-token")).thenReturn(claims);
        when(orderItemRepository.findByOrderNumberAndTicketId(orderNumber, 999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tickets/{orderNumber}/{ticketId}/qr", orderNumber, 999L)
                        .param("token", "valid-token"))
                .andExpect(status().isNotFound());
    }
}
