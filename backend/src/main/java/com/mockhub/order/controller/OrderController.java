package com.mockhub.order.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order creation and management")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService,
                           UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout", description = "Convert the current cart into an order. " +
            "Supply an Idempotency-Key header to prevent duplicate orders on retry.")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "200", description = "Existing order returned (idempotent retry)")
    @ApiResponse(responseCode = "400", description = "Cart is empty or invalid")
    public ResponseEntity<OrderDto> checkout(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User user = resolveUser(securityUser);
        OrderDto orderDto = orderService.checkout(user, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDto);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Return the current user's order history with pagination")
    @ApiResponse(responseCode = "200", description = "Orders returned")
    public ResponseEntity<PagedResponse<OrderSummaryDto>> listOrders(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(orderService.listOrders(user, page, size));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Return full details for a specific order")
    @ApiResponse(responseCode = "200", description = "Order returned")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderDto> getOrder(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String orderNumber) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(orderService.getOrder(user, orderNumber));
    }

    private User resolveUser(SecurityUser securityUser) {
        return userRepository.findByEmail(securityUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", securityUser.getEmail()));
    }
}
