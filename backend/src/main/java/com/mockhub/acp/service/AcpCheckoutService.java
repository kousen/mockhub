package com.mockhub.acp.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpLineItem;
import com.mockhub.acp.dto.AcpLineItemResponse;
import com.mockhub.acp.dto.AcpPricing;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.service.OrderService;


@Service
public class AcpCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(AcpCheckoutService.class);
    private static final String CURRENCY_USD = "USD";

    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final EventService eventService;

    public AcpCheckoutService(UserRepository userRepository,
                              CartService cartService,
                              OrderService orderService,
                              EventService eventService) {
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.eventService = eventService;
    }

    @Transactional
    public AcpCheckoutResponse createCheckout(AcpCheckoutRequest request) {
        User user = resolveUser(request.buyerEmail());

        String paymentMethod = request.paymentMethod() != null ? request.paymentMethod() : "mock";

        // Note: On idempotent retries, cart mutations below are harmless —
        // OrderService.checkout() returns the existing order before reading the cart.
        cartService.clearCart(user);
        for (AcpLineItem lineItem : request.lineItems()) {
            cartService.addToCart(user, lineItem.listingId());
        }

        // Create order via existing checkout flow (returns existing order on idempotent retry)
        CheckoutRequest checkoutRequest = new CheckoutRequest(paymentMethod);
        OrderDto orderDto = orderService.checkout(user, checkoutRequest, request.idempotencyKey());

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

        // Collect listing IDs to remove
        java.util.Set<Long> removeIds = new java.util.HashSet<>();
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
            cartService.addToCart(user, listingId);
        }
        if (request.addItems() != null) {
            for (AcpLineItem lineItem : request.addItems()) {
                cartService.addToCart(user, lineItem.listingId());
            }
        }

        // Create new order
        CheckoutRequest checkoutRequest = new CheckoutRequest("mock");
        OrderDto newOrderDto = orderService.checkout(user, checkoutRequest, null);

        log.info("ACP checkout updated, new order: {}", newOrderDto.orderNumber());

        return toAcpCheckoutResponse(newOrderDto, buyerEmail);
    }

    @Transactional
    public AcpCheckoutResponse completeCheckout(String checkoutId, String buyerEmail) {
        User user = resolveUser(buyerEmail);
        // Verify the user owns this order
        orderService.getOrder(user, checkoutId);

        orderService.confirmOrder(checkoutId);

        OrderDto confirmedOrder = orderService.getOrder(user, checkoutId);

        log.info("ACP checkout completed: {}", confirmedOrder.orderNumber());

        return toAcpCheckoutResponse(confirmedOrder, buyerEmail);
    }

    @Transactional
    public AcpCheckoutResponse cancelCheckout(String checkoutId, String buyerEmail) {
        User user = resolveUser(buyerEmail);
        // Verify the user owns this order
        OrderDto orderDto = orderService.getOrder(user, checkoutId);

        orderService.failOrder(checkoutId);

        log.info("ACP checkout cancelled: {}", orderDto.orderNumber());

        // Return response with CANCELLED status (failOrder sets FAILED internally,
        // but ACP uses CANCELLED terminology)
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
                "CANCELLED",
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
                query, category, null, city, null, null, null, null, "ACTIVE", "eventDate", page, size
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

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Buyer email is required");
        }
        return userRepository.findByEmail(email.strip())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
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
            case "FAILED" -> "CANCELLED";
            default -> orderStatus;
        };
    }
}
