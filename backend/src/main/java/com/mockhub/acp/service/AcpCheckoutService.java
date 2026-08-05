package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.acp.dto.AcpActionRequest;
import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpCompleteRequest;
import com.mockhub.acp.dto.AcpLineItem;
import com.mockhub.acp.dto.AcpLineItemResponse;
import com.mockhub.acp.dto.AcpPricing;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.commerce.dto.CommercePolicyDto;
import com.mockhub.commerce.service.CommercePolicyService;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.dto.OrderPricing;
import com.mockhub.order.service.OrderService;
import com.mockhub.order.service.PaymentMethodSupport;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;
import com.mockhub.paymentcredential.service.PaymentCredentialService;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;


@Service
public class AcpCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(AcpCheckoutService.class);
    private static final String CURRENCY_USD = "USD";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final ListingRepository listingRepository;
    private final EvalRunner evalRunner;
    private final PaymentService paymentService;
    private final CommercePolicyService commercePolicyService;
    private final AgentPurchaseApprovalService approvalService;
    private final MandateService mandateService;
    private final PaymentCredentialService paymentCredentialService;
    private final AgentRiskService agentRiskService;

    public AcpCheckoutService(UserRepository userRepository,
                              CartService cartService,
                              OrderService orderService,
                              ListingRepository listingRepository,
                              EvalRunner evalRunner,
                              PaymentService paymentService,
                              CommercePolicyService commercePolicyService,
                              AgentPurchaseApprovalService approvalService,
                              MandateService mandateService,
                              PaymentCredentialService paymentCredentialService,
                              AgentRiskService agentRiskService) {
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.listingRepository = listingRepository;
        this.evalRunner = evalRunner;
        this.paymentService = paymentService;
        this.commercePolicyService = commercePolicyService;
        this.approvalService = approvalService;
        this.mandateService = mandateService;
        this.paymentCredentialService = paymentCredentialService;
        this.agentRiskService = agentRiskService;
    }

    @Transactional
    public AcpCheckoutResponse createCheckout(AcpCheckoutRequest request) {
        User user = resolveUser(request.buyerEmail());
        String agentId = request.agentId().strip();
        String mandateId = request.mandateId().strip();

        String paymentMethod = PaymentMethodSupport.normalizeOrMock(request.paymentMethod());

        // Note: On idempotent retries, cart mutations below are harmless —
        // OrderService.checkout() returns the existing order before reading the cart.
        cartService.clearCart(user);
        for (AcpLineItem lineItem : request.lineItems()) {
            validateListingForAgent(user.getEmail(), lineItem.listingId(), agentId, mandateId);
            cartService.addToCart(user, lineItem.listingId());
        }
        validateCartForAgent(user, agentId, mandateId);
        CartDto cartDto = cartService.getCartDto(user);
        agentRiskService.recordCheckoutAttempt(
                user.getEmail(), agentId, null, cartDto.subtotal(), "ACP_CREATE_CHECKOUT");

        // Create order via existing checkout flow (returns existing order on idempotent retry)
        CheckoutRequest checkoutRequest = new CheckoutRequest(paymentMethod);
        OrderDto orderDto = orderService.checkout(
                user, checkoutRequest, request.idempotencyKey(), agentId, mandateId);

        log.info("ACP checkout created: {}", orderDto.orderNumber());

        return toAcpCheckoutResponse(orderDto, request.buyerEmail());
    }

    @Transactional(readOnly = true)
    public AcpCheckoutResponse getCheckout(String checkoutId, String buyerEmail) {
        User user = resolveUser(buyerEmail);
        OrderDto orderDto = orderService.getOrder(user, checkoutId);
        return toAcpCheckoutResponse(orderDto, buyerEmail);
    }

    @Transactional
    public AcpCheckoutResponse updateCheckout(String checkoutId, AcpUpdateRequest request, String buyerEmail) {
        User user = resolveUser(buyerEmail);
        OrderDto orderDto = orderService.getOrder(user, checkoutId);

        if (!"PENDING".equals(orderDto.status())) {
            throw new ConflictException(
                    "Cannot update checkout " + checkoutId + " with status " + orderDto.status()
                            + ". Only PENDING checkouts can be updated. Create a new checkout instead.");
        }

        Order order = orderService.getOrderEntity(checkoutId);
        agentRiskService.recordCheckoutAttempt(
                user.getEmail(), request.agentId(), checkoutId, order.getTotal(), "ACP_COMPLETE_CHECKOUT");
        validateStoredAgentContext(order, request.agentId(), request.mandateId());
        ensureOrderStillAuthorizedForConfirmation(order, user.getEmail(), request.agentId(), request.mandateId());

        // Collect listing IDs to remove
        Set<Long> removeIds = new HashSet<>();
        if (request.removeListingIds() != null) {
            removeIds.addAll(request.removeListingIds());
        }

        // Preserve existing items minus removals, then add new items
        List<Long> keepListingIds = orderDto.items().stream()
                .map(OrderItemDto::listingId)
                .filter(id -> !removeIds.contains(id))
                .toList();

        // Cancel the existing order (releases tickets)
        orderService.failOrder(checkoutId);

        // Rebuild cart with kept items + new items
        cartService.clearCart(user);
        for (Long listingId : keepListingIds) {
            validateListingForAgent(user.getEmail(), listingId, request.agentId(), request.mandateId());
            cartService.addToCart(user, listingId);
        }
        if (request.addItems() != null) {
            for (AcpLineItem lineItem : request.addItems()) {
                validateListingForAgent(user.getEmail(), lineItem.listingId(), request.agentId(), request.mandateId());
                cartService.addToCart(user, lineItem.listingId());
            }
        }
        validateCartForAgent(user, request.agentId(), request.mandateId());

        // Create new order with the original payment method preserved.
        CheckoutRequest checkoutRequest = new CheckoutRequest(PaymentMethodSupport.normalizeOrMock(order.getPaymentMethod()));
        OrderDto newOrderDto = orderService.checkout(
                user, checkoutRequest, null, request.agentId(), request.mandateId());

        log.info("ACP checkout updated, new order: {}", newOrderDto.orderNumber());

        return toAcpCheckoutResponse(newOrderDto, buyerEmail);
    }

    @Transactional
    public AcpCheckoutResponse completeCheckout(String checkoutId, String buyerEmail, AcpCompleteRequest request) {
        User user = resolveUser(buyerEmail);
        // Verify the user owns this order
        orderService.getOrder(user, checkoutId);
        Order order = orderService.getOrderEntity(checkoutId);
        validateStoredAgentContext(order, request.agentId(), request.mandateId());
        validateApprovalMode(user.getEmail(), request.agentId(), request.mandateId(), request.approvalId());
        approvalService.validateApprovedForCompletion(
                request.approvalId(), user.getEmail(), request.agentId(), request.mandateId(), order);
        // Last line of defence before money moves: re-authorize against the order total.
        // updateCheckout already did this; completion — the step that actually charges the
        // buyer — did not, so a mandate could be revoked or outgrown between the two calls.
        ensureOrderStillAuthorizedForConfirmation(
                order, user.getEmail(), request.agentId(), request.mandateId());

        String paymentIntentId = request.paymentIntentId();

        // Prevent cross-checkout payment intent swapping: if the order already has a stored
        // payment intent, the caller-supplied one must match it
        String storedIntent = order.getPaymentIntentId();
        if (storedIntent != null && paymentIntentId != null && !paymentIntentId.isBlank()
                && !storedIntent.equals(paymentIntentId.strip())) {
            throw new ConflictException("Payment intent does not belong to this checkout");
        }

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            order.setPaymentIntentId(paymentIntentId.strip());
        }

        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().strip().toLowerCase() : "mock";
        String paymentCredentialId;
        try {
            paymentCredentialId = validatePaymentCredentialIfPresent(
                    request.paymentCredentialId(), user.getEmail(), request.agentId(), order, paymentMethod);
        } catch (RuntimeException e) {
            agentRiskService.recordPaymentCredentialFailure(
                    user.getEmail(), request.agentId(), checkoutId, order.getTotal(), e.getMessage());
            throw e;
        }

        if ("mock".equals(paymentMethod)
                && (paymentIntentId == null || paymentIntentId.isBlank())
                && order.getPaymentIntentId() == null) {
            PaymentIntentDto paymentIntent = paymentService.createPaymentIntent(order);
            paymentIntentId = paymentIntent.paymentIntentId();
        } else if (paymentIntentId == null || paymentIntentId.isBlank()) {
            paymentIntentId = order.getPaymentIntentId();
        }

        if (!"mock".equals(paymentMethod) && (paymentIntentId == null || paymentIntentId.isBlank())) {
            throw new ConflictException("A payment intent is required to complete checkout for " + order.getPaymentMethod());
        }

        consumePaymentCredentialIfPresent(paymentCredentialId, order.getOrderNumber());

        try {
            paymentService.confirmPayment(paymentIntentId);
        } catch (RuntimeException e) {
            approvalService.markFailed(request.approvalId(), e.getMessage());
            throw e;
        }
        approvalService.markCompleted(request.approvalId(), checkoutId);

        OrderDto confirmedOrder = orderService.getOrder(user, checkoutId);

        log.info("ACP checkout completed: {}", confirmedOrder.orderNumber());

        return toAcpCheckoutResponse(confirmedOrder, buyerEmail);
    }

    @Transactional
    public AcpCheckoutResponse cancelCheckout(String checkoutId, String buyerEmail, AcpActionRequest request) {
        User user = resolveUser(buyerEmail);
        // Verify the user owns this order
        OrderDto orderDto = orderService.getOrder(user, checkoutId);
        Order order = orderService.getOrderEntity(checkoutId);
        validateStoredAgentContext(order, request.agentId(), request.mandateId());

        orderService.failOrder(checkoutId);

        log.info("ACP checkout cancelled: {}", orderDto.orderNumber());

        // Return response with CANCELLED status for explicit checkout cancellation.
        List<AcpLineItemResponse> lineItems = orderDto.items().stream()
                .map(item -> new AcpLineItemResponse(
                        item.listingId(),
                        item.eventName(),
                        item.eventSlug(),
                        item.sectionName(),
                        item.rowLabel(),
                        item.seatNumber(),
                        item.pricePaid(),
                        1
                ))
                .toList();

        AcpPricing pricing = new AcpPricing(
                orderDto.subtotal(),
                orderDto.serviceFee(),
                orderDto.total(),
                CURRENCY_USD
        );

        return new AcpCheckoutResponse(
                orderDto.orderNumber(),
                STATUS_CANCELLED,
                buyerEmail,
                lineItems,
                pricing,
                commercePolicyForLineItems(lineItems),
                orderDto.createdAt(),
                null
        );
    }

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Buyer email is required");
        }
        return userRepository.findByEmail(email.strip())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateListingForAgent(String userEmail, Long listingId, String agentId, String mandateId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        String categorySlug = listing.getEvent().getCategory() != null
                ? listing.getEvent().getCategory().getSlug()
                : null;
        // Authorize against what the buyer will be charged, service fee included — not the
        // ticket price alone, or a mandate ceiling silently permits a larger purchase.
        BigDecimal amountToAuthorize = OrderPricing.totalForSubtotal(listing.getComputedPrice());
        EvalSummary evalSummary = evalRunner.evaluate(EvalContext.forAgentAction(
                agentId, userEmail, listing.getEvent(), listing,
                amountToAuthorize, categorySlug, mandateId));

        if (evalSummary.hasCriticalFailure()) {
            agentRiskService.recordEvalFailures(userEmail, agentId, mandateId,
                    "ACP_LISTING_VALIDATION", "LISTING", listingId.toString(),
                    listing.getComputedPrice(), evalSummary);
            String failureMessage = evalSummary.failures().stream()
                    .map(result -> result.conditionName() + ": " + result.message())
                    .collect(Collectors.joining("; "));
            throw new ConflictException("ACP listing validation failed: " + failureMessage);
        }
    }

    private void validateCartForAgent(User user, String agentId, String mandateId) {
        CartDto cartDto = cartService.getCartDto(user);
        EvalSummary evalSummary = evalRunner.evaluate(EvalContext.forCart(cartDto));

        if (evalSummary.hasCriticalFailure()) {
            String failureMessage = evalSummary.failures().stream()
                    .map(result -> result.conditionName() + ": " + result.message())
                    .collect(Collectors.joining("; "));
            throw new ConflictException("ACP cart validation failed: " + failureMessage);
        }

        if (cartDto.items() == null || cartDto.items().isEmpty()) {
            return;
        }

        for (CartItemDto item : cartDto.items()) {
            BigDecimal subtotal = cartDto.subtotal() != null ? cartDto.subtotal() : item.currentPrice();
            if (subtotal == null) {
                subtotal = item.priceAtAdd();
            }
            // The buyer pays subtotal + service fee, so that is what the mandate must cover.
            BigDecimal amount = OrderPricing.totalForSubtotal(subtotal);
            boolean authorized = mandateService.validateAction(
                    agentId, user.getEmail(), "PURCHASE", amount, null,
                    item.eventSlug(), mandateId, item.sectionName());
            if (!authorized) {
                agentRiskService.recordMandateMismatch(
                        user.getEmail(), agentId, mandateId, "ACP_CART_VALIDATION", "LISTING",
                        item.listingId() != null ? item.listingId().toString() : null, amount,
                        "Mandate does not authorize cart item listing " + item.listingId());
                throw new ConflictException(
                        "ACP cart validation failed: Mandate does not authorize cart item listing "
                                + item.listingId());
            }
        }
    }

    private void validateStoredAgentContext(Order order, String agentId, String mandateId) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("Agent ID is required");
        }
        if (mandateId == null || mandateId.isBlank()) {
            throw new IllegalArgumentException("Mandate ID is required");
        }
        if (!agentId.strip().equals(order.getAgentId())) {
            throw new ConflictException("Agent ID does not match the checkout's recorded agent context");
        }
        if (!mandateId.strip().equals(order.getMandateId())) {
            throw new ConflictException("Mandate ID does not match the checkout's recorded mandate context");
        }
    }

    private void validateApprovalMode(String userEmail, String agentId, String mandateId, String approvalId) {
        if (mandateService.approvalRequired(agentId.strip(), userEmail, mandateId.strip())
                && (approvalId == null || approvalId.isBlank())) {
            throw new ConflictException("Mandate requires an approved purchase approval before completion");
        }
    }

    private String validatePaymentCredentialIfPresent(String paymentCredentialId, String userEmail,
                                                      String agentId, Order order, String paymentMethod) {
        if (paymentCredentialId == null || paymentCredentialId.isBlank()) {
            return null;
        }
        String normalizedPaymentCredentialId = paymentCredentialId.strip();
        paymentCredentialService.authorizeForPayment(
                normalizedPaymentCredentialId,
                userEmail,
                agentId.strip(),
                order.getTotal(),
                CURRENCY_USD,
                paymentMethod,
                order.getOrderNumber());
        return normalizedPaymentCredentialId;
    }

    private void consumePaymentCredentialIfPresent(String paymentCredentialId, String orderNumber) {
        if (paymentCredentialId != null) {
            paymentCredentialService.consumeForPayment(paymentCredentialId, orderNumber);
        }
    }

    private void ensureOrderStillAuthorizedForConfirmation(Order order, String userEmail,
                                                           String agentId, String mandateId) {
        List<com.mockhub.eval.dto.EvalResult> failures = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            String categorySlug = item.getListing().getEvent().getCategory() != null
                    ? item.getListing().getEvent().getCategory().getSlug()
                    : null;
            EvalSummary summary = evalRunner.evaluate(EvalContext.forAgentAction(
                    agentId.strip(), userEmail, item.getListing().getEvent(), item.getListing(),
                    order.getTotal(), categorySlug, mandateId.strip()));
            if (summary.hasCriticalFailure()) {
                agentRiskService.recordEvalFailures(userEmail, agentId.strip(), mandateId.strip(),
                        "ACP_CONFIRMATION_VALIDATION", "ORDER", order.getOrderNumber(), order.getTotal(), summary);
                failures.addAll(summary.failures());
            }
        }

        if (!failures.isEmpty()) {
            String failureMessage = failures.stream()
                    .map(result -> result.conditionName() + ": " + result.message())
                    .collect(Collectors.joining("; "));
            throw new ConflictException("ACP confirmation validation failed: " + failureMessage);
        }
    }

    private AcpCheckoutResponse toAcpCheckoutResponse(OrderDto orderDto, String buyerEmail) {
        List<AcpLineItemResponse> lineItems = orderDto.items().stream()
                .map(item -> new AcpLineItemResponse(
                        item.listingId(),
                        item.eventName(),
                        item.eventSlug(),
                        item.sectionName(),
                        item.rowLabel(),
                        item.seatNumber(),
                        item.pricePaid(),
                        1
                ))
                .toList();

        AcpPricing pricing = new AcpPricing(
                orderDto.subtotal(),
                orderDto.serviceFee(),
                orderDto.total(),
                CURRENCY_USD
        );

        String acpStatus = mapOrderStatusToAcpStatus(orderDto.status());

        return new AcpCheckoutResponse(
                orderDto.orderNumber(),
                acpStatus,
                buyerEmail,
                lineItems,
                pricing,
                commercePolicyForLineItems(lineItems),
                orderDto.createdAt(),
                orderDto.confirmedAt()
        );
    }

    private CommercePolicyDto commercePolicyForLineItems(List<AcpLineItemResponse> lineItems) {
        Set<String> eventSlugs = lineItems.stream()
                .map(AcpLineItemResponse::eventSlug)
                .filter(slug -> slug != null && !slug.isBlank())
                .collect(Collectors.toSet());

        if (eventSlugs.size() == 1) {
            return commercePolicyService.getPolicyForEvent(eventSlugs.iterator().next());
        }

        return commercePolicyService.getDefaultPolicy();
    }

    private String mapOrderStatusToAcpStatus(String orderStatus) {
        return switch (orderStatus) {
            case "PENDING" -> "CREATED";
            case "CONFIRMED" -> "COMPLETED";
            case "FAILED", STATUS_CANCELLED -> STATUS_CANCELLED;
            default -> orderStatus;
        };
    }
}
