package com.mockhub.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.cart.entity.Cart;
import com.mockhub.cart.entity.CartItem;
import com.mockhub.cart.repository.CartRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.TicketService;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ORDER_RESOURCE = "Order";
    private static final String ORDER_NUMBER_FIELD = "orderNumber";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final TicketService ticketService;
    private final EventRepository eventRepository;
    private final OrderNotificationService orderNotificationService;
    private final MandateService mandateService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartService cartService,
                        TicketService ticketService,
                        EventRepository eventRepository,
                        OrderNotificationService orderNotificationService,
                        MandateService mandateService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.orderNotificationService = orderNotificationService;
        this.mandateService = mandateService;
    }

    @Transactional
    @SuppressWarnings("java:S6809") // Self-invocation is intentional — simple delegation to the full overload
    public OrderDto checkout(User user, CheckoutRequest request, String idempotencyKey) {
        return checkout(user, request, idempotencyKey, null, null);
    }

    @Transactional
    public OrderDto checkout(User user, CheckoutRequest request, String idempotencyKey,
                             String agentId, String mandateId) {
        // Idempotency check: if a key was provided and an order already exists, return it
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Idempotent retry detected for key {}, returning existing order {}",
                        idempotencyKey, existingOrder.get().getOrderNumber());
                return toOrderDto(existingOrder.get());
            }
        }

        List<CartItem> cartItems = validateAndReserveTickets(user);
        Order order = createOrder(user, request, cartItems, idempotencyKey, agentId, mandateId);

        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} for user {} with {} items, total={}",
                order.getOrderNumber(), user.getId(), order.getItems().size(), order.getTotal());

        cartService.clearCart(user);

        return toOrderDto(savedOrder);
    }

    @Transactional
    public void confirmOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));

        if (STATUS_CONFIRMED.equals(order.getStatus())) {
            log.info("Order {} is already confirmed; skipping duplicate confirmation", orderNumber);
            return;
        }

        if (STATUS_FAILED.equals(order.getStatus()) || STATUS_CANCELLED.equals(order.getStatus())) {
            throw new ConflictException("Cannot confirm " + order.getStatus().toLowerCase() + " order " + orderNumber);
        }

        order.setStatus(STATUS_CONFIRMED);
        order.setConfirmedAt(Instant.now());
        markTicketsAsSold(order);
        orderRepository.save(order);
        log.info("Confirmed order {}", order.getOrderNumber());

        if (order.getMandateId() != null && !order.getMandateId().isBlank()) {
            mandateService.recordSpend(order.getMandateId(), order.getTotal());
        }

        orderNotificationService.sendConfirmationNotifications(order);
    }

    @Transactional
    public void failOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));

        if (STATUS_FAILED.equals(order.getStatus())) {
            log.info("Order {} is already failed; skipping duplicate failure handling",
                    order.getOrderNumber());
            return;
        }

        if (STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new ConflictException("Cannot fail confirmed order " + orderNumber);
        }

        order.setStatus(STATUS_FAILED);
        releaseOrderTickets(order);
        orderRepository.save(order);
        log.info("Failed order {}, released {} tickets", order.getOrderNumber(), order.getItems().size());
    }

    @Transactional
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));

        if (STATUS_CANCELLED.equals(order.getStatus())) {
            log.info("Order {} is already cancelled; skipping duplicate cancellation", orderNumber);
            return;
        }

        if (!STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new ConflictException("Can only cancel confirmed orders");
        }

        order.setStatus(STATUS_CANCELLED);
        releaseOrderTickets(order);
        restoreEventAvailability(order);

        if (order.getMandateId() != null && !order.getMandateId().isBlank()) {
            mandateService.reverseSpend(order.getMandateId(), order.getTotal());
        }

        orderRepository.save(order);
        log.info("Cancelled order {}, released {} tickets", order.getOrderNumber(), order.getItems().size());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(User user, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this order");
        }

        return toOrderDto(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryDto> listOrders(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<OrderSummaryDto> summaries = orderPage.getContent().stream()
                .map(order -> {
                    String eventName = null;
                    Instant eventDate = null;
                    String venueName = null;
                    if (!order.getItems().isEmpty()) {
                        long distinctEvents = order.getItems().stream()
                                .map(item -> item.getListing().getEvent().getId())
                                .distinct()
                                .count();
                        if (distinctEvents == 1) {
                            com.mockhub.event.entity.Event event =
                                    order.getItems().getFirst().getListing().getEvent();
                            eventName = event.getName();
                            eventDate = event.getEventDate();
                            if (event.getVenue() != null) {
                                venueName = event.getVenue().getName();
                            }
                        } else {
                            eventName = "Multiple events";
                        }
                    }
                    return new OrderSummaryDto(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getStatus(),
                            order.getTotal(),
                            order.getItems().size(),
                            order.getCreatedAt(),
                            eventName,
                            eventDate,
                            venueName
                    );
                })
                .toList();

        return new PagedResponse<>(
                summaries,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));
    }

    @Transactional(readOnly = true)
    public Order getOrderEntityWithItems(String orderNumber) {
        return orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));
    }

    @Transactional
    public Order getOrderEntityByPaymentIntentId(String paymentIntentId) {
        return orderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, "paymentIntentId", paymentIntentId));
    }

    @Transactional
    public Order getOrderEntityForUpdate(String orderNumber) {
        return orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, ORDER_NUMBER_FIELD, orderNumber));
    }

    @Transactional
    public Order getOrderEntityByPaymentIntentIdForUpdate(String paymentIntentId) {
        return orderRepository.findByPaymentIntentIdForUpdate(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_RESOURCE, "paymentIntentId", paymentIntentId));
    }

    // ── Checkout helpers ───────────────────────────────────────────────

    private List<CartItem> validateAndReserveTickets(User user) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ConflictException("No cart found"));

        List<CartItem> cartItems = cart.getItems();
        if (cartItems.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }

        // Validate and reserve each item in the same loop to minimize TOCTOU window
        for (CartItem cartItem : cartItems) {
            Listing listing = cartItem.getListing();
            if (!"ACTIVE".equals(listing.getStatus())) {
                throw new ConflictException(
                        "Listing for " + listing.getEvent().getName() + " is no longer available");
            }

            Ticket ticket = listing.getTicket();
            if (!"AVAILABLE".equals(ticket.getStatus()) && !"LISTED".equals(ticket.getStatus())) {
                throw new ConflictException(
                        "Ticket for " + listing.getEvent().getName() + " is no longer available");
            }

            ticketService.reserveTicket(ticket.getId());
        }

        return cartItems;
    }

    private Order createOrder(User user, CheckoutRequest request, List<CartItem> cartItems,
                               String idempotencyKey, String agentId, String mandateId) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            subtotal = subtotal.add(cartItem.getListing().getComputedPrice());
        }
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(serviceFee);

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(STATUS_PENDING);
        order.setSubtotal(subtotal);
        order.setServiceFee(serviceFee);
        order.setTotal(total);
        order.setPaymentMethod(request.paymentMethod());
        order.setAgentId(normalize(agentId));
        order.setMandateId(normalize(mandateId));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            order.setIdempotencyKey(idempotencyKey);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setListing(cartItem.getListing());
            orderItem.setTicket(cartItem.getListing().getTicket());
            orderItem.setPricePaid(cartItem.getListing().getComputedPrice());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        return order;
    }

    private void markTicketsAsSold(Order order) {
        for (OrderItem item : order.getItems()) {
            item.getTicket().setStatus("SOLD");
            item.getListing().setStatus("SOLD");

            Event event = item.getListing().getEvent();
            event.setAvailableTickets(Math.max(0, event.getAvailableTickets() - 1));
            eventRepository.save(event);
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────

    private void releaseOrderTickets(Order order) {
        for (OrderItem item : order.getItems()) {
            ticketService.releaseTicket(item.getTicket().getId());
            item.getListing().setStatus("ACTIVE");
        }
    }

    private void restoreEventAvailability(Order order) {
        for (OrderItem item : order.getItems()) {
            Event event = item.getListing().getEvent();
            event.setAvailableTickets(event.getAvailableTickets() + 1);
            eventRepository.save(event);
        }
    }

    private String generateOrderNumber() {
        String dateStr = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
        String prefix = "MH-" + dateStr + "-";

        long sequence = orderRepository.findMaxOrderNumberByPrefix(prefix)
                .map(maxOrderNumber -> {
                    String suffix = maxOrderNumber.substring(prefix.length());
                    return Long.parseLong(suffix) + 1;
                })
                .orElse(1L);

        return String.format("%s%04d", prefix, sequence);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private OrderDto toOrderDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    Listing listing = item.getListing();
                    Ticket ticket = item.getTicket();

                    String rowLabel = null;
                    String seatNumber = null;
                    if (ticket.getSeat() != null) {
                        rowLabel = ticket.getSeat().getRow().getRowLabel();
                        seatNumber = ticket.getSeat().getSeatNumber();
                    }

                    return new OrderItemDto(
                            item.getId(),
                            listing.getId(),
                            ticket.getId(),
                            listing.getEvent().getName(),
                            listing.getEvent().getSlug(),
                            ticket.getSection().getName(),
                            rowLabel,
                            seatNumber,
                            ticket.getTicketType(),
                            item.getPricePaid()
                    );
                })
                .toList();

        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotal(),
                order.getServiceFee(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getConfirmedAt(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}
