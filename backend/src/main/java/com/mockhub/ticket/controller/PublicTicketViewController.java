package com.mockhub.ticket.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.dto.PublicOrderViewDto;
import com.mockhub.ticket.dto.PublicTicketViewDto;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.QrCodeService;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.Venue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/v1/tickets")
public class PublicTicketViewController {

    private static final Logger log = LoggerFactory.getLogger(PublicTicketViewController.class);

    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");

    private final TicketSigningService ticketSigningService;
    private final QrCodeService qrCodeService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final String verificationBaseUrl;

    public PublicTicketViewController(TicketSigningService ticketSigningService,
                                      QrCodeService qrCodeService,
                                      OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      @Value("${mockhub.ticket.verification-base-url}") String verificationBaseUrl) {
        this.ticketSigningService = ticketSigningService;
        this.qrCodeService = qrCodeService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @GetMapping("/view")
    @Transactional(readOnly = true)
    public ResponseEntity<PublicOrderViewDto> viewTickets(@RequestParam String token) {
        Claims claims;
        try {
            claims = ticketSigningService.verifyOrderViewToken(token);
        } catch (JwtException exception) {
            log.warn("Order view token verification failed: {}", exception.getMessage());
            return ResponseEntity.badRequest().build();
        }

        String orderNumber = claims.getSubject();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            return ResponseEntity.status(409).build();
        }

        Event event = order.getItems().getFirst().getListing().getEvent();
        Venue venue = event.getVenue();

        String eventDate = event.getEventDate()
                .atZone(DISPLAY_ZONE)
                .format(EVENT_DATE_FORMATTER);

        List<PublicTicketViewDto> tickets = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Ticket ticket = item.getTicket();
            String sectionName = ticket.getSection().getName();

            String rowLabel = null;
            String seatNumber = null;
            Seat seat = ticket.getSeat();
            if (seat != null) {
                rowLabel = seat.getRow().getRowLabel();
                seatNumber = seat.getSeatNumber();
            }

            String qrCodeUrl = "/api/v1/tickets/" + orderNumber + "/" + ticket.getId()
                    + "/qr?token=" + token;

            tickets.add(new PublicTicketViewDto(
                    ticket.getId(),
                    sectionName,
                    rowLabel,
                    seatNumber,
                    ticket.getTicketType(),
                    qrCodeUrl
            ));
        }

        PublicOrderViewDto dto = new PublicOrderViewDto(
                orderNumber,
                order.getStatus().name(),
                event.getName(),
                eventDate,
                venue.getName(),
                venue.getCity() + ", " + venue.getState(),
                tickets
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{orderNumber}/{ticketId}/qr")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String orderNumber,
                                             @PathVariable Long ticketId,
                                             @RequestParam String token) {
        Claims claims;
        try {
            claims = ticketSigningService.verifyOrderViewToken(token);
        } catch (JwtException exception) {
            log.warn("QR code token verification failed: {}", exception.getMessage());
            return ResponseEntity.badRequest().build();
        }

        if (!orderNumber.equals(claims.getSubject())) {
            return ResponseEntity.badRequest().build();
        }

        OrderItem orderItem = orderItemRepository
                .findByOrderNumberAndTicketId(orderNumber, ticketId)
                .orElse(null);

        if (orderItem == null) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = orderItem.getTicket();
        String eventSlug = orderItem.getListing().getEvent().getSlug();
        String sectionName = ticket.getSection().getName();

        String rowLabel = null;
        String seatNumber = null;
        Seat seat = ticket.getSeat();
        if (seat != null) {
            rowLabel = seat.getRow().getRowLabel();
            seatNumber = seat.getSeatNumber();
        }

        String ticketToken = ticketSigningService.generateToken(
                orderNumber, ticketId, eventSlug, sectionName, rowLabel, seatNumber);

        String verificationUrl = verificationBaseUrl + "/api/v1/tickets/verify?token=" + ticketToken;
        byte[] qrImage = qrCodeService.generateQrCode(verificationUrl, 300);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}
