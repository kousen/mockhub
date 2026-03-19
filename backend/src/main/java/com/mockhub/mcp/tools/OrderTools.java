package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.service.OrderService;

@Component
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public OrderTools(OrderService orderService,
                      UserRepository userRepository,
                      ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Create an order by checking out a user's cart. "
            + "Validates all listings are still available, calculates totals with service fees, "
            + "reserves tickets, and creates a pending order. Cart is cleared after checkout.")
    public String checkout(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Payment method identifier (e.g. 'stripe', 'mock')",
                    required = true) String paymentMethod) {
        try {
            if (paymentMethod == null || paymentMethod.isBlank()) {
                return errorJson("Payment method is required");
            }
            User user = resolveUser(userEmail);
            CheckoutRequest request = new CheckoutRequest(paymentMethod.strip());
            OrderDto order = orderService.checkout(user, request, null);
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            log.error("Error during checkout for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to checkout: " + e.getMessage());
        }
    }

    @Tool(description = "Get details of a specific order by order number. "
            + "Returns order status, items, pricing breakdown, and confirmation details.")
    public String getOrder(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Order number (e.g. 'MH-20260319-0001')", required = true) String orderNumber) {
        try {
            if (orderNumber == null || orderNumber.isBlank()) {
                return errorJson("Order number is required");
            }
            User user = resolveUser(userEmail);
            OrderDto order = orderService.getOrder(user, orderNumber.strip());
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            log.error("Error getting order '{}' for '{}': {}", orderNumber, userEmail, e.getMessage(), e);
            return errorJson("Failed to get order: " + e.getMessage());
        }
    }

    @Tool(description = "List a user's orders with pagination. "
            + "Returns order summaries with order number, status, total, item count, and date.")
    public String listOrders(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Page number (0-based), defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size (1-100), defaults to 20", required = false) Integer size) {
        try {
            User user = resolveUser(userEmail);
            int pageNum = (page == null || page < 0) ? 0 : page;
            int pageSize = (size == null || size <= 0) ? 20 : Math.min(size, 100);
            PagedResponse<OrderSummaryDto> orders = orderService.listOrders(user, pageNum, pageSize);
            return objectMapper.writeValueAsString(orders);
        } catch (Exception e) {
            log.error("Error listing orders for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to list orders: " + e.getMessage());
        }
    }

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        return userRepository.findByEmail(email.strip())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
