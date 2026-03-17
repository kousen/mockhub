package com.mockhub.payment.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.order.entity.Order;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.ConfirmPaymentRequest;
import com.mockhub.payment.dto.CreatePaymentIntentRequest;
import com.mockhub.payment.dto.PaymentConfirmation;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and webhooks")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService,
                             OrderService orderService,
                             UserRepository userRepository) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-intent")
    @Operation(summary = "Create payment intent", description = "Create a Stripe payment intent for an order")
    @ApiResponse(responseCode = "201", description = "Payment intent created")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "403", description = "Not authorized to pay for this order")
    public ResponseEntity<PaymentIntentDto> createPaymentIntent(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        User user = resolveUser(securityUser);
        Order order = orderService.getOrderEntity(request.orderNumber());

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this order");
        }

        PaymentIntentDto paymentIntent = paymentService.createPaymentIntent(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentIntent);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment", description = "Confirm a payment intent has been completed")
    @ApiResponse(responseCode = "200", description = "Payment confirmed")
    @ApiResponse(responseCode = "400", description = "Payment failed")
    public ResponseEntity<PaymentConfirmation> confirmPayment(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        // Validate user is authenticated (securityUser not null is ensured by Spring Security)
        PaymentConfirmation confirmation = paymentService.confirmPayment(request.paymentIntentId());
        return ResponseEntity.ok(confirmation);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe webhook", description = "Receive and process Stripe webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    private User resolveUser(SecurityUser securityUser) {
        return userRepository.findByEmail(securityUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", securityUser.getEmail()));
    }
}
