package com.mockhub.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mockhub.auth.entity.Role;
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
import com.mockhub.notification.service.EmailDeliveryService;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.service.NotificationService;
import com.mockhub.notification.service.SmsDeliveryService;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.dto.TicketDto;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.TicketService;
import com.mockhub.venue.entity.Section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartService cartService;

    @Mock
    private TicketService ticketService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SmsDeliveryService smsDeliveryService;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private TicketSigningService ticketSigningService;

    @Mock
    private MandateService mandateService;

    private OrderService orderService;

    private User testUser;
    private User otherUser;
    private Cart testCart;
    private Order testOrder;
    private Listing testListing;
    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartRepository, cartService,
                ticketService, eventRepository, notificationService,
                smsDeliveryService, emailDeliveryService, ticketSigningService, mandateService,
                "http://localhost:5173");

        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRoles(Set.of(buyerRole));

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setRoles(Set.of(buyerRole));

        Event testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");
        testEvent.setAvailableTickets(10);

        Section testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Floor");

        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setTicketType("GENERAL_ADMISSION");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("LISTED");

        testListing = new Listing();
        testListing.setId(1L);
        testListing.setTicket(testTicket);
        testListing.setEvent(testEvent);
        testListing.setListedPrice(new BigDecimal("75.00"));
        testListing.setComputedPrice(new BigDecimal("75.00"));
        testListing.setPriceMultiplier(BigDecimal.ONE);
        testListing.setStatus("ACTIVE");
        testListing.setListedAt(Instant.now());

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setListing(testListing);
        cartItem.setPriceAtAdd(new BigDecimal("75.00"));
        cartItem.setAddedAt(Instant.now());

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>(List.of(cartItem)));
        testCart.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        cartItem.setCart(testCart);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setOrderNumber("MH-20260317-0001");
        testOrder.setStatus("PENDING");
        testOrder.setSubtotal(new BigDecimal("75.00"));
        testOrder.setServiceFee(new BigDecimal("7.50"));
        testOrder.setTotal(new BigDecimal("82.50"));
        testOrder.setPaymentMethod("mock");
        testOrder.setCreatedAt(Instant.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(testOrder);
        orderItem.setListing(testListing);
        orderItem.setTicket(testTicket);
        orderItem.setPricePaid(new BigDecimal("75.00"));
        testOrder.setItems(List.of(orderItem));
    }

    @Test
    @DisplayName("checkout - given cart with items - creates order and clears cart")
    void checkout_givenCartWithItems_createsOrderAndClearsCart() {
        CheckoutRequest request = new CheckoutRequest("mock");

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(ticketService.reserveTicket(anyLong())).thenReturn(
                new TicketDto(1L, 1L, "Floor", null, null, "GA", new BigDecimal("50.00"), "RESERVED"));
        when(orderRepository.findMaxOrderNumberByPrefix(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(Instant.now());
            return order;
        });

        OrderDto result = orderService.checkout(testUser, request, null);

        assertNotNull(result, "Order DTO should not be null");
        assertEquals("PENDING", result.status(), "Order status should be PENDING");
        verify(cartService).clearCart(testUser);
    }

    @Test
    @DisplayName("checkout - given empty cart - throws ConflictException")
    void checkout_givenEmptyCart_throwsConflictException() {
        testCart.setItems(new ArrayList<>());
        CheckoutRequest request = new CheckoutRequest("mock");

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        assertThrows(ConflictException.class,
                () -> orderService.checkout(testUser, request, null),
                "Should throw ConflictException for empty cart");
    }

    @Test
    @DisplayName("checkout - given no cart - throws ConflictException")
    void checkout_givenNoCart_throwsConflictException() {
        CheckoutRequest request = new CheckoutRequest("mock");

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class,
                () -> orderService.checkout(testUser, request, null),
                "Should throw ConflictException when no cart exists");
    }

    @Test
    @DisplayName("checkout - given duplicate idempotency key - returns existing order")
    void checkout_givenDuplicateIdempotencyKey_returnsExistingOrder() {
        Order existingOrder = new Order();
        existingOrder.setId(99L);
        existingOrder.setUser(testUser);
        existingOrder.setOrderNumber("MH-20260319-0001");
        existingOrder.setStatus("PENDING");
        existingOrder.setSubtotal(new BigDecimal("75.00"));
        existingOrder.setServiceFee(new BigDecimal("7.50"));
        existingOrder.setTotal(new BigDecimal("82.50"));
        existingOrder.setPaymentMethod("STRIPE");
        existingOrder.setIdempotencyKey("idem-key-123");
        existingOrder.setItems(new ArrayList<>());
        existingOrder.setCreatedAt(Instant.now());

        when(orderRepository.findByIdempotencyKey("idem-key-123"))
                .thenReturn(Optional.of(existingOrder));

        CheckoutRequest request = new CheckoutRequest("STRIPE");
        OrderDto result = orderService.checkout(testUser, request, "idem-key-123");

        assertNotNull(result);
        assertEquals("MH-20260319-0001", result.orderNumber());
        verify(cartRepository, org.mockito.Mockito.never()).findByUser(any());
    }

    @Test
    @DisplayName("checkout - given null idempotency key - creates new order normally")
    void checkout_givenNullIdempotencyKey_createsNewOrderNormally() {
        CheckoutRequest request = new CheckoutRequest("mock");

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(orderRepository.findMaxOrderNumberByPrefix(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(Instant.now());
            return order;
        });

        OrderDto result = orderService.checkout(testUser, request, null);

        assertNotNull(result);
        verify(orderRepository, org.mockito.Mockito.never()).findByIdempotencyKey(any());
    }

    @Test
    @DisplayName("confirmOrder - duplicate confirmation is idempotent")
    void confirmOrder_givenDuplicateConfirmation_isIdempotent() {
        testOrder.setMandateId("mandate-123");
        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.confirmOrder("MH-20260317-0001");
        orderService.confirmOrder("MH-20260317-0001");

        assertEquals("CONFIRMED", testOrder.getStatus());
        assertNotNull(testOrder.getConfirmedAt());
        verify(notificationService, times(1)).createNotification(
                anyLong(),
                org.mockito.ArgumentMatchers.eq(NotificationType.ORDER_CONFIRMED),
                anyString(),
                anyString(),
                anyString());
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(mandateService, times(1)).recordSpend("mandate-123", new BigDecimal("82.50"));
    }

    @Test
    @DisplayName("failOrder - duplicate failure is idempotent")
    void failOrder_givenDuplicateFailure_isIdempotent() {
        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.failOrder("MH-20260317-0001");
        orderService.failOrder("MH-20260317-0001");

        assertEquals("FAILED", testOrder.getStatus());
        verify(ticketService, times(1)).releaseTicket(1L);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("confirmOrder - failed order cannot be confirmed")
    void confirmOrder_givenFailedOrder_throwsConflictException() {
        testOrder.setStatus("FAILED");
        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));

        assertThrows(ConflictException.class,
                () -> orderService.confirmOrder("MH-20260317-0001"));

        verify(notificationService, org.mockito.Mockito.never())
                .createNotification(anyLong(), org.mockito.ArgumentMatchers.any(NotificationType.class),
                        anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("failOrder - confirmed order cannot be failed")
    void failOrder_givenConfirmedOrder_throwsConflictException() {
        testOrder.setStatus("CONFIRMED");
        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));

        assertThrows(ConflictException.class,
                () -> orderService.failOrder("MH-20260317-0001"));

        verify(ticketService, org.mockito.Mockito.never()).releaseTicket(anyLong());
    }

    @Test
    @DisplayName("getOrder - given own order number - returns order DTO")
    void getOrder_givenOwnOrderNumber_returnsOrderDto() {
        when(orderRepository.findByOrderNumber("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.getOrder(testUser, "MH-20260317-0001");

        assertNotNull(result, "Order DTO should not be null");
        assertEquals("MH-20260317-0001", result.orderNumber(), "Order number should match");
    }

    @Test
    @DisplayName("getOrder - given other user's order - throws UnauthorizedException")
    void getOrder_givenOtherUsersOrder_throwsUnauthorizedException() {
        when(orderRepository.findByOrderNumber("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));

        assertThrows(UnauthorizedException.class,
                () -> orderService.getOrder(otherUser, "MH-20260317-0001"),
                "Should throw UnauthorizedException for other user's order");
    }

    @Test
    @DisplayName("getOrder - given unknown order number - throws ResourceNotFoundException")
    void getOrder_givenUnknownOrderNumber_throwsResourceNotFoundException() {
        when(orderRepository.findByOrderNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrder(testUser, "NONEXISTENT"),
                "Should throw ResourceNotFoundException for unknown order");
    }

    @Test
    @DisplayName("cancelOrder - given confirmed order with mandate - reverses spend and releases tickets")
    void cancelOrder_givenConfirmedOrderWithMandate_reversesSpendAndReleasesTickets() {
        testOrder.setStatus("CONFIRMED");
        testOrder.setMandateId("mandate-123");
        Event event = testOrder.getItems().getFirst().getListing().getEvent();
        int originalAvailable = event.getAvailableTickets();

        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.cancelOrder("MH-20260317-0001");

        assertEquals("CANCELLED", testOrder.getStatus());
        verify(ticketService).releaseTicket(1L);
        verify(eventRepository).save(event);
        assertEquals(originalAvailable + 1, event.getAvailableTickets());
        verify(mandateService).reverseSpend("mandate-123", new BigDecimal("82.50"));
    }

    @Test
    @DisplayName("cancelOrder - given confirmed order without mandate - releases tickets only")
    void cancelOrder_givenConfirmedOrderWithoutMandate_releasesTicketsOnly() {
        testOrder.setStatus("CONFIRMED");
        testOrder.setMandateId(null);

        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.cancelOrder("MH-20260317-0001");

        assertEquals("CANCELLED", testOrder.getStatus());
        verify(ticketService).releaseTicket(1L);
        verify(mandateService, never()).reverseSpend(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("cancelOrder - given pending order - throws ConflictException")
    void cancelOrder_givenPendingOrder_throwsConflictException() {
        testOrder.setStatus("PENDING");
        when(orderRepository.findByOrderNumberForUpdate("MH-20260317-0001"))
                .thenReturn(Optional.of(testOrder));

        assertThrows(ConflictException.class,
                () -> orderService.cancelOrder("MH-20260317-0001"));

        verify(ticketService, never()).releaseTicket(anyLong());
    }

    @Test
    @DisplayName("listOrders - given user with orders - returns paged response")
    void listOrders_givenUserWithOrders_returnsPagedResponse() {
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<OrderSummaryDto> result = orderService.listOrders(testUser, 0, 20);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one order");
    }
}
