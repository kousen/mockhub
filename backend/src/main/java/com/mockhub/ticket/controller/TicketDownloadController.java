package com.mockhub.ticket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.service.TicketPdfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Ticket Downloads", description = "Download ticket PDFs for confirmed orders")
public class TicketDownloadController {

    private static final Logger log = LoggerFactory.getLogger(TicketDownloadController.class);

    private final TicketPdfService ticketPdfService;
    private final OrderItemRepository orderItemRepository;

    public TicketDownloadController(TicketPdfService ticketPdfService,
                                    OrderItemRepository orderItemRepository) {
        this.ticketPdfService = ticketPdfService;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/{orderNumber}/tickets/{ticketId}/download")
    @Operation(summary = "Download ticket PDF",
            description = "Download a PDF ticket for a specific ticket in a confirmed order")
    @ApiResponse(responseCode = "200", description = "Ticket PDF returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated or not the order owner")
    @ApiResponse(responseCode = "404", description = "Order item not found")
    @ApiResponse(responseCode = "409", description = "Order is not confirmed")
    public ResponseEntity<byte[]> downloadTicket(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String orderNumber,
            @PathVariable Long ticketId) {

        log.debug("Downloading ticket PDF for order {} ticket {}", orderNumber, ticketId);

        OrderItem orderItem = orderItemRepository.findByOrderNumberAndTicketId(orderNumber, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!orderItem.getOrder().getUser().getId().equals(securityUser.getId())) {
            throw new UnauthorizedException("You do not own this order");
        }

        if (orderItem.getOrder().getStatus() != com.mockhub.order.entity.OrderStatus.CONFIRMED) {
            throw new ConflictException("Order is not confirmed");
        }

        byte[] pdfBytes = ticketPdfService.generateTicketPdf(orderItem);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + orderNumber + "-" + ticketId + ".pdf")
                .body(pdfBytes);
    }
}
