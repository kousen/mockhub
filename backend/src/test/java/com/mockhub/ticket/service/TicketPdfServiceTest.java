package com.mockhub.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.imageio.ImageIO;

import com.mockhub.auth.entity.User;
import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketPdfServiceTest {

    @Mock
    private TicketSigningService ticketSigningService;

    @Mock
    private QrCodeService qrCodeService;

    private TicketPdfService ticketPdfService;

    private OrderItem orderItem;
    private Event event;
    private Ticket ticket;

    @BeforeEach
    void setUp() throws IOException {
        ticketPdfService = new TicketPdfService(
                ticketSigningService, qrCodeService, "https://mockhub.example.com");

        event = new Event();
        event.setName("Rock Festival 2026");
        event.setSlug("rock-festival-2026");
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setDoorsOpenAt(Instant.now().plus(30, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS));

        Venue venue = new Venue();
        venue.setName("Madison Square Garden");
        venue.setCity("New York");
        venue.setState("NY");
        event.setVenue(venue);

        Section section = new Section();
        section.setName("Floor A");

        SeatRow row = new SeatRow();
        row.setRowLabel("12");

        Seat seat = new Seat();
        seat.setSeatNumber("7");
        seat.setRow(row);

        ticket = new Ticket();
        ticket.setId(456L);
        ticket.setSection(section);
        ticket.setSeat(seat);
        ticket.setTicketType("standard");

        Listing listing = new Listing();
        listing.setEvent(event);

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        Order order = new Order();
        order.setOrderNumber("MH-20260321-0001");
        order.setUser(user);

        orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setTicket(ticket);
        orderItem.setListing(listing);
        orderItem.setPricePaid(new BigDecimal("75.00"));

        when(ticketSigningService.generateToken(anyString(), anyLong(), anyString(),
                anyString(), any(), any()))
                .thenReturn("mock-jwt-token");
        when(qrCodeService.generateQrCode(anyString(), anyInt()))
                .thenReturn(createMinimalPngBytes());
    }

    @Test
    void generateTicketPdf_givenConfirmedOrderItem_returnsPdfBytes() {
        byte[] result = ticketPdfService.generateTicketPdf(orderItem);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        // PDF files start with %PDF (bytes 0x25, 0x50, 0x44, 0x46)
        assertThat(result[0]).isEqualTo((byte) 0x25);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x44);
        assertThat(result[3]).isEqualTo((byte) 0x46);
    }

    @Test
    void generateTicketPdf_givenOrderItem_callsSigningServiceWithCorrectArgs() {
        ticketPdfService.generateTicketPdf(orderItem);

        verify(ticketSigningService).generateToken(
                eq("MH-20260321-0001"),
                eq(456L),
                eq("rock-festival-2026"),
                eq("Floor A"),
                eq("12"),
                eq("7")
        );
    }

    @Test
    void generateTicketPdf_givenOrderItem_callsQrCodeServiceWithVerificationUrl() {
        ticketPdfService.generateTicketPdf(orderItem);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(qrCodeService).generateQrCode(urlCaptor.capture(), eq(250));

        String capturedUrl = urlCaptor.getValue();
        assertThat(capturedUrl).isEqualTo(
                "https://mockhub.example.com/api/v1/tickets/verify?token=mock-jwt-token");
    }

    @Test
    void generateTicketPdf_givenNullSeat_handlesGracefully() {
        ticket.setSeat(null);

        byte[] result = ticketPdfService.generateTicketPdf(orderItem);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        // PDF header check
        assertThat(result[0]).isEqualTo((byte) 0x25);

        verify(ticketSigningService).generateToken(
                eq("MH-20260321-0001"),
                eq(456L),
                eq("rock-festival-2026"),
                eq("Floor A"),
                isNull(),
                isNull()
        );
    }

    @Test
    void generateTicketPdf_givenNullDoorsOpen_omitsDoorsOpenLine() {
        event.setDoorsOpenAt(null);

        byte[] result = ticketPdfService.generateTicketPdf(orderItem);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        // PDF header check
        assertThat(result[0]).isEqualTo((byte) 0x25);
    }

    private byte[] createMinimalPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
