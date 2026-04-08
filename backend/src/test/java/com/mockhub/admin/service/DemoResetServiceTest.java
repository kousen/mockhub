package com.mockhub.admin.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.mockhub.admin.dto.DemoResetResultDto;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MandateService mandateService;

    @Mock
    private MandateRepository mandateRepository;

    @InjectMocks
    private DemoResetService demoResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@mockhub.com");
        testUser.setFirstName("Jane");
        testUser.setLastName("Buyer");
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("resetUser - user not found - throws ResourceNotFoundException")
    void resetUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@mockhub.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> demoResetService.resetUser("unknown@mockhub.com"));
    }

    @Test
    @DisplayName("resetUser - clears cart for the user")
    void resetUser_clearsCart() {
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(mandateRepository.findByUserEmail("buyer@mockhub.com"))
                .thenReturn(Collections.emptyList());

        demoResetService.resetUser("buyer@mockhub.com");

        verify(cartService).clearCart(testUser);
    }

    @Test
    @DisplayName("resetUser - fails pending orders via orderService")
    void resetUser_failsPendingOrders() {
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(testUser));

        Order pendingOrder = mock(Order.class);
        when(pendingOrder.getStatus()).thenReturn(OrderStatus.PENDING);
        when(pendingOrder.getOrderNumber()).thenReturn("MH-20260408-0001");

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));
        when(mandateRepository.findByUserEmail("buyer@mockhub.com"))
                .thenReturn(Collections.emptyList());

        demoResetService.resetUser("buyer@mockhub.com");

        verify(orderService).failOrder("MH-20260408-0001");
    }

    @Test
    @DisplayName("resetUser - cancels confirmed orders via orderService")
    void resetUser_cancelsConfirmedOrders() {
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(testUser));

        Order confirmedOrder = mock(Order.class);
        when(confirmedOrder.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        when(confirmedOrder.getOrderNumber()).thenReturn("MH-20260408-0002");

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(confirmedOrder)));
        when(mandateRepository.findByUserEmail("buyer@mockhub.com"))
                .thenReturn(Collections.emptyList());

        demoResetService.resetUser("buyer@mockhub.com");

        verify(orderService).cancelOrder("MH-20260408-0002");
    }

    @Test
    @DisplayName("resetUser - revokes active mandates via mandateService")
    void resetUser_revokesActiveMandates() {
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Mandate activeMandate = new Mandate();
        activeMandate.setMandateId("mandate-001");
        activeMandate.setStatus("ACTIVE");

        when(mandateRepository.findByUserEmail("buyer@mockhub.com"))
                .thenReturn(List.of(activeMandate));

        demoResetService.resetUser("buyer@mockhub.com");

        verify(mandateService).revokeMandate("mandate-001");
    }

    @Test
    @DisplayName("resetUser - full reset - returns summary with correct counts")
    void resetUser_fullReset_returnsSummary() {
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(testUser));

        Order pendingOrder = mock(Order.class);
        when(pendingOrder.getStatus()).thenReturn(OrderStatus.PENDING);
        when(pendingOrder.getOrderNumber()).thenReturn("MH-20260408-0001");

        Order confirmedOrder = mock(Order.class);
        when(confirmedOrder.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        when(confirmedOrder.getOrderNumber()).thenReturn("MH-20260408-0002");

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(pendingOrder, confirmedOrder)));

        Mandate activeMandate = new Mandate();
        activeMandate.setMandateId("mandate-001");
        activeMandate.setStatus("ACTIVE");

        Mandate revokedMandate = new Mandate();
        revokedMandate.setMandateId("mandate-002");
        revokedMandate.setStatus("REVOKED");

        when(mandateRepository.findByUserEmail("buyer@mockhub.com"))
                .thenReturn(List.of(activeMandate, revokedMandate));

        DemoResetResultDto result = demoResetService.resetUser("buyer@mockhub.com");

        assertNotNull(result, "Result should not be null");
        assertEquals("buyer@mockhub.com", result.userEmail(), "Email should match");
        assertTrue(result.cartCleared(), "Cart should be marked as cleared");
        assertEquals(2, result.cancelledOrders().size(),
                "Should have 2 cancelled/failed orders");
        assertTrue(result.cancelledOrders().contains("MH-20260408-0001"),
                "Should contain the pending order number");
        assertTrue(result.cancelledOrders().contains("MH-20260408-0002"),
                "Should contain the confirmed order number");
        assertEquals(1, result.revokedMandates().size(),
                "Should have 1 revoked mandate (the already-revoked one is skipped)");
        assertTrue(result.revokedMandates().contains("mandate-001"),
                "Should contain the active mandate ID");
    }
}
