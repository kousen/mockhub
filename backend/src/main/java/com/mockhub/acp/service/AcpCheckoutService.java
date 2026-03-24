package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpActionRequest;
import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpCompleteRequest;
import com.mockhub.acp.dto.AcpListingItem;
import com.mockhub.acp.dto.AcpLineItem;
import com.mockhub.acp.dto.AcpLineItemResponse;
import com.mockhub.acp.dto.AcpPricing;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;


@Service
public class AcpCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(AcpCheckoutService.class);
    private static final String CURRENCY_USD = "USD";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final EventService eventService;
    private final ListingRepository listingRepository;
    private final EvalRunner evalRunner;
    private final PaymentService paymentService;

    public AcpCheckoutService(UserRepository userRepository,
                              CartService cartService,
                              OrderService orderService,
                              EventService eventService,
                              ListingRepository listingRepository,
                              EvalRunner evalRunner,
                              PaymentService paymentService) {
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.eventService = eventService;
        this.listingRepository = listingRepository;
        this.evalRunner = evalRunner;
        this.paymentService = paymentService;
    }

    @Transactional
    public AcpCheckoutResponse createCheckout(AcpCheckoutRequest request) {
        User user = resolveUser(request.buyerEmail());
        String agentId = request.agentId().strip();
        String mandateId = request.mandateId().strip();

        String paymentMethod = request.paymentMethod() != null ? request.paymentMethod() : "mock";

        // Note: On idempotent retries, cart mutations below are harmless —
        // OrderService.checkout() returns the existing order before reading the cart.
        cartService.clearCart(user);
        for (AcpLineItem lineItem : request.lineItems()) {
            validateListingForAgent(user.getEmail(), lineItem.listingId(), agentId, mandateId);
            cartService.addToCart(user, lineItem.listingId());
        }
        validateCartForAgent(user, agentId, mandateId);

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

        // Create new order
        CheckoutRequest checkoutRequest = new CheckoutRequest("mock");
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

        paymentService.confirmPayment(paymentIntentId);

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
                orderDto.createdAt(),
                null
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AcpCatalogItem> getCatalog(String query, String category,
                                                     String city, int page, int size) {
        EventSearchRequest searchRequest = new EventSearchRequest(
                query, category, null, city, null, null, null, null, STATUS_ACTIVE, "eventDate", page, size
        );

        PagedResponse<EventSummaryDto> eventPage = eventService.listEvents(searchRequest);

        List<AcpCatalogItem> catalogItems = eventPage.content().stream()
                .map(event -> new AcpCatalogItem(
                        event.slug(),
                        event.name(),
                        event.artistName() != null ? event.artistName() : event.name(),
                        event.categoryName(),
                        event.venueName(),
                        event.city(),
                        event.eventDate(),
                        event.minPrice(),
                        event.minPrice(), // EventSummaryDto only has minPrice
                        event.availableTickets(),
                        "/events/" + event.slug()
                ))
                .toList();

        return new PagedResponse<>(
                catalogItems,
                eventPage.page(),
                eventPage.size(),
                eventPage.totalElements(),
                eventPage.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AcpListingItem> getListings(String query, String category,
                                                     String city, Instant dateFrom, Instant dateTo,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     String section, int page, int size) {
        EventSearchRequest searchRequest = new EventSearchRequest(
                query, category, null, city,
                dateFrom, dateTo, minPrice, maxPrice,
                STATUS_ACTIVE, "eventDate", 0, 100
        );

        PagedResponse<EventSummaryDto> eventPage = eventService.listEvents(searchRequest);
        List<AcpListingItem> allListings = new ArrayList<>();

        for (EventSummaryDto event : eventPage.content()) {
            List<Listing> eventListings = listingRepository.findByEventIdAndStatus(event.id(), STATUS_ACTIVE);
            for (Listing listing : eventListings) {
                if (!matchesFilters(listing, minPrice, maxPrice, section)) {
                    continue;
                }

                String rowLabel = null;
                String seatNumber = null;
                if (listing.getTicket().getSeat() != null) {
                    rowLabel = listing.getTicket().getSeat().getRow().getRowLabel();
                    seatNumber = listing.getTicket().getSeat().getSeatNumber();
                }

                allListings.add(new AcpListingItem(
                        listing.getId(),
                        event.slug(),
                        event.name(),
                        event.slug(),
                        event.artistName() != null ? event.artistName() : event.name(),
                        event.categoryName(),
                        event.venueName(),
                        event.city(),
                        event.eventDate(),
                        listing.getTicket().getSection().getName(),
                        rowLabel,
                        seatNumber,
                        listing.getComputedPrice(),
                        "/events/" + event.slug()
                ));
            }
        }

        List<AcpListingItem> sortedListings = allListings.stream()
                .sorted(Comparator.comparing(AcpListingItem::price))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int start = Math.min(safePage * safeSize, sortedListings.size());
        int end = Math.min(start + safeSize, sortedListings.size());
        int totalPages = sortedListings.isEmpty() ? 0 : (int) Math.ceil((double) sortedListings.size() / safeSize);

        return new PagedResponse<>(
                sortedListings.subList(start, end),
                safePage,
                safeSize,
                sortedListings.size(),
                totalPages
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
        EvalSummary evalSummary = evalRunner.evaluate(EvalContext.forAgentAction(
                agentId, userEmail, listing.getEvent(), listing,
                listing.getComputedPrice(), categorySlug, mandateId));

        if (evalSummary.hasCriticalFailure()) {
            String failureMessage = evalSummary.failures().stream()
                    .map(result -> result.conditionName() + ": " + result.message())
                    .collect(Collectors.joining("; "));
            throw new ConflictException("ACP listing validation failed: " + failureMessage);
        }
    }

    private void validateCartForAgent(User user, String agentId, String mandateId) {
        CartDto cartDto = cartService.getCartDto(user);
        EvalSummary evalSummary = evalRunner.evaluate(EvalContext.forAgentAction(
                agentId, user.getEmail(), null, null, cartDto.subtotal(), null, mandateId));

        if (evalSummary.hasCriticalFailure()) {
            String failureMessage = evalSummary.failures().stream()
                    .map(result -> result.conditionName() + ": " + result.message())
                    .collect(Collectors.joining("; "));
            throw new ConflictException("ACP cart validation failed: " + failureMessage);
        }
    }

    private boolean matchesFilters(Listing listing, BigDecimal minPrice,
                                   BigDecimal maxPrice, String section) {
        boolean priceAboveMin = minPrice == null || listing.getComputedPrice().compareTo(minPrice) >= 0;
        boolean priceBelowMax = maxPrice == null || listing.getComputedPrice().compareTo(maxPrice) <= 0;
        boolean sectionMatches = section == null || section.isBlank()
                || section.strip().equalsIgnoreCase(listing.getTicket().getSection().getName());
        return priceAboveMin && priceBelowMax && sectionMatches;
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
                orderDto.createdAt(),
                orderDto.confirmedAt()
        );
    }

    private String mapOrderStatusToAcpStatus(String orderStatus) {
        return switch (orderStatus) {
            case "PENDING" -> "CREATED";
            case "CONFIRMED" -> "COMPLETED";
            case "FAILED", "CANCELLED" -> STATUS_CANCELLED;
            default -> orderStatus;
        };
    }
}
