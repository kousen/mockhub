package com.mockhub.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.service.NotificationService;
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

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final TicketService ticketService;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartService cartService,
                        TicketService ticketService,
                        EventRepository eventRepository,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderDto checkout(User user, CheckoutRequest request) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ConflictException("No cart found"));

        List<CartItem> cartItems = cart.getItems();
        if (cartItems.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }

        // Validate all listings are still ACTIVE and reserve tickets
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

            // Reserve the ticket
            ticketService.reserveTicket(ticket.getId());
        }

        // Calculate pricing
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            subtotal = subtotal.add(cartItem.getListing().getComputedPrice());
        }
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(serviceFee);

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(orderNumber);
        order.setStatus("PENDING");
        order.setSubtotal(subtotal);
        order.setServiceFee(serviceFee);
        order.setTotal(total);
        order.setPaymentMethod(request.paymentMethod());

        // Create order items
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

        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} for user {} with {} items, total={}",
                orderNumber, user.getId(), orderItems.size(), total);

        // Clear the cart
        cartService.clearCart(user);

        return toOrderDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(User user, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

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
                .map(order -> new OrderSummaryDto(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        order.getTotal(),
                        order.getItems().size(),
                        order.getCreatedAt()
                ))
                .toList();

        return new PagedResponse<>(
                summaries,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional
    public void confirmOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        order.setStatus("CONFIRMED");
        order.setConfirmedAt(Instant.now());

        // Update ticket statuses to SOLD and decrement event available tickets
        for (OrderItem item : order.getItems()) {
            Ticket ticket = item.getTicket();
            ticket.setStatus("SOLD");

            Event event = item.getListing().getEvent();
            event.setAvailableTickets(Math.max(0, event.getAvailableTickets() - 1));
            eventRepository.save(event);
        }

        orderRepository.save(order);
        log.info("Confirmed order {}", orderNumber);

        // Send order confirmation notification
        notificationService.createNotification(
                order.getUser().getId(),
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                String.format("Your order %s has been confirmed. Total: $%s",
                        orderNumber, order.getTotal().toPlainString()),
                "/orders/" + orderNumber
        );
    }

    @Transactional
    public void failOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        order.setStatus("FAILED");

        // Release reserved tickets back to AVAILABLE
        for (OrderItem item : order.getItems()) {
            ticketService.releaseTicket(item.getTicket().getId());
        }

        orderRepository.save(order);
        log.info("Failed order {}, released {} tickets", orderNumber, order.getItems().size());
    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
    }

    private String generateOrderNumber() {
        String dateStr = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
        String prefix = "MH-" + dateStr + "-";

        long count = orderRepository.countByOrderNumberPrefix(prefix);
        long sequence = count + 1;

        return String.format("%s%04d", prefix, sequence);
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
