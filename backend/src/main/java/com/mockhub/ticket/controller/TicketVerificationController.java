package com.mockhub.ticket.controller;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.dto.TicketVerificationResult;
import com.mockhub.ticket.service.TicketSigningService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(TicketVerificationController.class);

    private final TicketSigningService ticketSigningService;
    private final OrderItemRepository orderItemRepository;

    public TicketVerificationController(TicketSigningService ticketSigningService,
                                        OrderItemRepository orderItemRepository) {
        this.ticketSigningService = ticketSigningService;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/verify")
    @Transactional
    public ResponseEntity<TicketVerificationResult> verifyTicket(@RequestParam String token) {
        Claims claims;
        try {
            claims = ticketSigningService.verifyToken(token);
        } catch (JwtException exception) {
            logger.warn("Ticket verification failed: {}", exception.getMessage());
            return ResponseEntity.ok(new TicketVerificationResult(
                    false, null, null, null, null, null, null,
                    false, null, "Invalid or tampered ticket"));
        }

        String orderNumber = claims.getSubject();
        Long ticketId = claims.get("tic", Long.class);
        String eventSlug = claims.get("evt", String.class);
        String sectionName = claims.get("sec", String.class);
        String rowLabel = claims.get("row", String.class);
        String seatNumber = claims.get("seat", String.class);

        Optional<OrderItem> orderItemOpt = orderItemRepository.findByOrderNumberAndTicketId(orderNumber, ticketId);

        if (orderItemOpt.isEmpty()) {
            logger.warn("Ticket not found: orderNumber={}, ticketId={}", orderNumber, ticketId);
            return ResponseEntity.ok(new TicketVerificationResult(
                    false, orderNumber, ticketId, eventSlug, sectionName, rowLabel, seatNumber,
                    false, null, "Ticket not found in system"));
        }

        OrderItem orderItem = orderItemOpt.get();

        if (orderItem.getScannedAt() != null) {
            logger.info("Ticket already scanned: orderNumber={}, ticketId={}, scannedAt={}",
                    orderNumber, ticketId, orderItem.getScannedAt());
            return ResponseEntity.ok(new TicketVerificationResult(
                    true, orderNumber, ticketId, eventSlug, sectionName, rowLabel, seatNumber,
                    true, orderItem.getScannedAt(),
                    "Ticket already scanned at " + orderItem.getScannedAt()));
        }

        orderItem.setScannedAt(Instant.now());
        logger.info("Ticket verified successfully: orderNumber={}, ticketId={}", orderNumber, ticketId);

        return ResponseEntity.ok(new TicketVerificationResult(
                true, orderNumber, ticketId, eventSlug, sectionName, rowLabel, seatNumber,
                false, null, "Ticket verified successfully"));
    }
}
