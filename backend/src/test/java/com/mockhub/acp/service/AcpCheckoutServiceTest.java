package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpLineItem;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.service.EventService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.service.OrderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcpCheckoutServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AcpCheckoutService acpCheckoutService;

    private User testUser;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@test.com");

        List<OrderItemDto> orderItems = List.of(
                new OrderItemDto(1L, 10L, 100L, "Test Concert", "test-concert",
                        "Floor", "A", "1", "GENERAL", new BigDecimal("50.00"))
        );

        testOrderDto = new OrderDto(
                1L,
                "MH-20260323-0001",
                "PENDING",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                null,
                Instant.now(),
                orderItems
        );
    }

    @Test
    @DisplayName("createCheckout - valid request - returns CREATED response")
    void createCheckout_validRequest_returnsCreatedResponse() {
        AcpCheckoutRequest request = new AcpCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "mock",
                "idem-123",
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(testUser, 10L)).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 1, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq("idem-123")))
                .thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);

        assertNotNull(response);
        assertEquals("MH-20260323-0001", response.checkoutId());
        assertEquals("CREATED", response.status());
        assertEquals("buyer@test.com", response.buyerEmail());
        assertEquals(1, response.lineItems().size());
        assertEquals("USD", response.pricing().currency());
        assertEquals(new BigDecimal("55.00"), response.pricing().total());

        verify(cartService).clearCart(testUser);
        verify(cartService).addToCart(testUser, 10L);
        verify(orderService).checkout(eq(testUser), any(CheckoutRequest.class), eq("idem-123"));
    }

    @Test
    @DisplayName("createCheckout - unknown buyer email - throws ResourceNotFoundException")
    void createCheckout_unknownBuyerEmail_throwsResourceNotFoundException() {
        AcpCheckoutRequest request = new AcpCheckoutRequest(
                "nobody@test.com",
                List.of(new AcpLineItem(10L, 1)),
                null,
                null,
                null
        );

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                acpCheckoutService.createCheckout(request));
    }

    @Test
    @DisplayName("createCheckout - multiple line items - adds all to cart")
    void createCheckout_multipleLineItems_addsAllToCart() {
        AcpCheckoutRequest request = new AcpCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1), new AcpLineItem(20L, 1)),
                null,
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 2, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null)))
                .thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);

        assertNotNull(response);
        verify(cartService).addToCart(testUser, 10L);
        verify(cartService).addToCart(testUser, 20L);
    }

    @Test
    @DisplayName("completeCheckout - pending order - returns COMPLETED response")
    void completeCheckout_pendingOrder_returnsCompletedResponse() {
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items()
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001"))
                .thenReturn(testOrderDto)
                .thenReturn(confirmedOrder);

        AcpCheckoutResponse response = acpCheckoutService.completeCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertNotNull(response.completedAt());

        verify(orderService).confirmOrder("MH-20260323-0001");
    }

    @Test
    @DisplayName("cancelCheckout - pending order - returns CANCELLED response")
    void cancelCheckout_pendingOrder_returnsCancelledResponse() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.cancelCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        assertNull(response.completedAt());

        verify(orderService).failOrder("MH-20260323-0001");
    }

    @Test
    @DisplayName("updateCheckout - non-pending order - throws ConflictException")
    void updateCheckout_nonPendingOrder_throwsConflictException() {
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items()
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(confirmedOrder);

        AcpUpdateRequest updateRequest = new AcpUpdateRequest(
                List.of(new AcpLineItem(30L, 1)),
                null
        );

        assertThrows(ConflictException.class, () ->
                acpCheckoutService.updateCheckout("MH-20260323-0001", updateRequest, "buyer@test.com"));
    }

    @Test
    @DisplayName("getCheckout - valid checkout - returns response")
    void getCheckout_validCheckout_returnsResponse() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.getCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertNotNull(response);
        assertEquals("MH-20260323-0001", response.checkoutId());
        assertEquals("CREATED", response.status());
        assertEquals("buyer@test.com", response.buyerEmail());
    }
}
