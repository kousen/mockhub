package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.service.CalendarService;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;

@Component
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private final OrderService orderService;
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final EvalRunner evalRunner;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderTools(OrderService orderService,
                      CalendarService calendarService,
                      UserRepository userRepository,
                      CartService cartService,
                      EvalRunner evalRunner,
                      PaymentService paymentService,
                      ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.evalRunner = evalRunner;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Create an order by checking out a user's cart. "
            + "Validates all listings are still available, calculates totals with service fees, "
            + "reserves tickets, and creates a pending order. Cart is cleared after checkout.")
    public String checkout(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Payment method identifier (e.g. 'stripe', 'mock')",
                    required = true) String paymentMethod,
            @ToolParam(description = "Agent ID performing the purchase action", required = true) String agentId,
            @ToolParam(description = "Active mandate ID authorizing the purchase action",
                    required = true) String mandateId) {
        try {
            if (paymentMethod == null || paymentMethod.isBlank()) {
                return errorJson("Payment method is required");
            }
            if (agentId == null || agentId.isBlank()) {
                return errorJson("Agent ID is required");
            }
            if (mandateId == null || mandateId.isBlank()) {
                return errorJson("Mandate ID is required");
            }
            User user = resolveUser(userEmail);
            CartDto cartDto = cartService.getCartDto(user);
            EvalSummary evalSummary = evalRunner.evaluate(EvalContext.forAgentAction(
                    agentId.strip(), user.getEmail(), null, null, cartDto.subtotal(), null, mandateId.strip()));
            if (evalSummary.hasCriticalFailure()) {
                String failureMessage = evalSummary.failures().stream()
                        .map(result -> result.conditionName() + ": " + result.message())
                        .collect(Collectors.joining("; "));
                return errorJson("Cannot checkout: " + failureMessage);
            }
            CheckoutRequest request = new CheckoutRequest(paymentMethod.strip());
            OrderDto order = orderService.checkout(user, request, null, agentId.strip(), mandateId.strip());
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

    @Tool(description = "Confirm a pending order, completing the purchase. "
            + "In mock-payment mode, this transitions the order from PENDING to CONFIRMED. "
            + "Returns the updated order with confirmation details.")
    @Transactional
    public String confirmOrder(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Order number (e.g. 'MH-20260319-0001')", required = true) String orderNumber,
            @ToolParam(description = "Agent ID performing the purchase action", required = true) String agentId,
            @ToolParam(description = "Active mandate ID authorizing the purchase action",
                    required = true) String mandateId,
            @ToolParam(description = "Existing payment intent ID for non-mock payment methods",
                    required = false) String paymentIntentId) {
        try {
            if (orderNumber == null || orderNumber.isBlank()) {
                return errorJson("Order number is required");
            }
            if (agentId == null || agentId.isBlank()) {
                return errorJson("Agent ID is required");
            }
            if (mandateId == null || mandateId.isBlank()) {
                return errorJson("Mandate ID is required");
            }
            User user = resolveUser(userEmail);
            String trimmedOrderNumber = orderNumber.strip();
            // Verify ownership before confirming — getOrder throws UnauthorizedException if mismatch
            orderService.getOrder(user, trimmedOrderNumber);
            Order order = orderService.getOrderEntity(trimmedOrderNumber);
            validateStoredAgentContext(order, agentId, mandateId);

            // Re-evaluate mandate authorization at confirmation time
            EvalSummary evalSummary = revalidateOrderForConfirmation(order, user.getEmail(), agentId, mandateId);
            if (evalSummary.hasCriticalFailure()) {
                String failureMessage = evalSummary.failures().stream()
                        .map(result -> result.conditionName() + ": " + result.message())
                        .collect(Collectors.joining("; "));
                return errorJson("Cannot confirm order: " + failureMessage);
            }

            String resolvedPaymentIntentId = resolvePaymentIntentId(order, paymentIntentId);
            paymentService.confirmPayment(resolvedPaymentIntentId);
            OrderDto confirmedOrder = orderService.getOrder(user, trimmedOrderNumber);
            return objectMapper.writeValueAsString(confirmedOrder);
        } catch (Exception e) {
            log.error("Error confirming order '{}' for '{}': {}", orderNumber, userEmail, e.getMessage(), e);
            return errorJson("Failed to confirm order: " + e.getMessage());
        }
    }

    @Tool(description = "Get a calendar (.ics) entry for a confirmed order. "
            + "Returns iCalendar content that can be imported into any calendar app. "
            + "Includes event name, date, venue address, doors-open time, and ticket details.")
    public String getCalendarEntry(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "Order number (e.g. 'MH-20260319-0001')", required = true) String orderNumber) {
        try {
            if (orderNumber == null || orderNumber.isBlank()) {
                return errorJson("Order number is required");
            }
            User user = resolveUser(userEmail);
            orderService.getOrder(user, orderNumber.strip()); // auth check
            Order order = orderService.getOrderEntityWithItems(orderNumber.strip());
            return calendarService.generateIcs(order);
        } catch (Exception e) {
            log.error("Error generating calendar for order '{}': {}", orderNumber, e.getMessage(), e);
            return errorJson("Failed to generate calendar entry: " + e.getMessage());
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

    private void validateStoredAgentContext(Order order, String agentId, String mandateId) {
        if (!agentId.strip().equals(order.getAgentId())) {
            throw new ConflictException("Agent ID does not match the order's recorded agent context");
        }
        if (!mandateId.strip().equals(order.getMandateId())) {
            throw new ConflictException("Mandate ID does not match the order's recorded mandate context");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private String resolvePaymentIntentId(Order order, String paymentIntentId) {
        String normalizedPaymentIntentId = normalize(paymentIntentId);
        if (normalizedPaymentIntentId != null) {
            order.setPaymentIntentId(normalizedPaymentIntentId);
        }

        String orderPaymentMethod = normalize(order.getPaymentMethod());
        if ("mock".equalsIgnoreCase(orderPaymentMethod)) {
            if (normalize(order.getPaymentIntentId()) == null) {
                PaymentIntentDto createdIntent = paymentService.createPaymentIntent(order);
                return createdIntent.paymentIntentId();
            }
            return order.getPaymentIntentId();
        }

        if (normalizedPaymentIntentId == null) {
            normalizedPaymentIntentId = normalize(order.getPaymentIntentId());
        }
        if (normalizedPaymentIntentId == null) {
            throw new ConflictException(
                    "Payment intent ID is required to confirm non-mock payment orders");
        }
        return normalizedPaymentIntentId;
    }

    private EvalSummary revalidateOrderForConfirmation(Order order, String userEmail,
                                                       String agentId, String mandateId) {
        List<EvalResult> failures = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            String categorySlug = item.getListing().getEvent().getCategory() != null
                    ? item.getListing().getEvent().getCategory().getSlug()
                    : null;
            EvalSummary itemSummary = evalRunner.evaluate(EvalContext.forAgentAction(
                    agentId.strip(), userEmail, item.getListing().getEvent(), item.getListing(),
                    order.getTotal(), categorySlug, mandateId.strip()));
            if (itemSummary.hasCriticalFailure()) {
                failures.addAll(itemSummary.failures());
            }
        }

        return new EvalSummary(failures);
    }
}
