package com.mockhub.payment.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.PaymentException;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.entity.TransactionLog;
import com.mockhub.payment.repository.TransactionLogRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private OrderService orderService;

    private StripePaymentService stripePaymentService;
    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        stripePaymentService = new StripePaymentService(
                "sk_test_fake", "whsec_test_fake",
                transactionLogRepository, orderService);

        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRoles(Set.of(buyerRole));

        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setOrderNumber("MH-20260319-0001");
        testOrder.setUser(testUser);
        testOrder.setSubtotal(new BigDecimal("150.00"));
        testOrder.setServiceFee(new BigDecimal("15.00"));
        testOrder.setTotal(new BigDecimal("165.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentMethod("STRIPE");
    }

    // === createPaymentIntent ===

    @Test
    @DisplayName("createPaymentIntent - given valid order - returns PaymentIntentDto")
    void createPaymentIntent_givenValidOrder_returnsPaymentIntentDto() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_123");
        when(mockIntent.getClientSecret()).thenReturn("cs_test_secret");

        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockIntent);

            var result = stripePaymentService.createPaymentIntent(testOrder);

            assertNotNull(result);
            assertEquals("pi_test_123", result.paymentIntentId());
            assertEquals("cs_test_secret", result.clientSecret());
            assertEquals(new BigDecimal("165.00"), result.amount());
            assertEquals("USD", result.currency());
            assertEquals("pi_test_123", testOrder.getPaymentIntentId());
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }
    }

    @Test
    @DisplayName("createPaymentIntent - given Stripe error - throws PaymentException")
    void createPaymentIntent_givenStripeError_throwsPaymentException() {
        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new StripeException("Card declined", null, null, 402) {});

            PaymentException exception = assertThrows(PaymentException.class,
                    () -> stripePaymentService.createPaymentIntent(testOrder));

            assertEquals("Failed to create payment intent: Card declined", exception.getMessage());
            verify(transactionLogRepository, never()).save(any());
        }
    }

    // === confirmPayment ===

    @Test
    @DisplayName("confirmPayment - given succeeded payment - confirms order and returns SUCCEEDED")
    void confirmPayment_givenSucceededPayment_confirmsOrderAndReturnSucceeded() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        when(orderService.getOrderEntityForUpdate("MH-20260319-0001")).thenReturn(testOrder);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(mockIntent);

            var result = stripePaymentService.confirmPayment("pi_test_123");

            assertEquals("pi_test_123", result.paymentIntentId());
            assertEquals("SUCCEEDED", result.status());
            assertEquals("MH-20260319-0001", result.orderNumber());
            verify(orderService).confirmOrder("MH-20260319-0001");
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }
    }

    @Test
    @DisplayName("confirmPayment - given duplicate succeeded payment - returns existing success without reprocessing")
    void confirmPayment_givenDuplicateSucceededPayment_returnsExistingSuccessWithoutReprocessing() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        Order confirmedOrder = new Order();
        confirmedOrder.setId(101L);
        confirmedOrder.setOrderNumber("MH-20260319-0001");
        confirmedOrder.setUser(testUser);
        confirmedOrder.setSubtotal(new BigDecimal("150.00"));
        confirmedOrder.setServiceFee(new BigDecimal("15.00"));
        confirmedOrder.setTotal(new BigDecimal("165.00"));
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        confirmedOrder.setPaymentMethod("STRIPE");

        when(orderService.getOrderEntityForUpdate("MH-20260319-0001"))
                .thenReturn(testOrder)
                .thenReturn(confirmedOrder);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(mockIntent);

            var first = stripePaymentService.confirmPayment("pi_test_123");
            var second = stripePaymentService.confirmPayment("pi_test_123");

            assertEquals("SUCCEEDED", first.status());
            assertEquals("SUCCEEDED", second.status());
            verify(orderService, times(1)).confirmOrder("MH-20260319-0001");
            verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
        }
    }

    @Test
    @DisplayName("confirmPayment - given failed payment - fails order and returns FAILED")
    void confirmPayment_givenFailedPayment_failsOrderAndReturnsFailed() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_payment_method");
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        when(orderService.getOrderEntityForUpdate("MH-20260319-0001")).thenReturn(testOrder);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(mockIntent);

            var result = stripePaymentService.confirmPayment("pi_test_123");

            assertEquals("FAILED", result.status());
            verify(orderService).failOrder("MH-20260319-0001");
        }
    }

    @Test
    @DisplayName("confirmPayment - given duplicate failed payment - returns existing failure without reprocessing")
    void confirmPayment_givenDuplicateFailedPayment_returnsExistingFailureWithoutReprocessing() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_payment_method");
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        Order failedOrder = new Order();
        failedOrder.setId(102L);
        failedOrder.setOrderNumber("MH-20260319-0001");
        failedOrder.setUser(testUser);
        failedOrder.setSubtotal(new BigDecimal("150.00"));
        failedOrder.setServiceFee(new BigDecimal("15.00"));
        failedOrder.setTotal(new BigDecimal("165.00"));
        failedOrder.setStatus(OrderStatus.FAILED);
        failedOrder.setPaymentMethod("STRIPE");

        when(orderService.getOrderEntityForUpdate("MH-20260319-0001"))
                .thenReturn(testOrder)
                .thenReturn(failedOrder);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(mockIntent);

            var first = stripePaymentService.confirmPayment("pi_test_123");
            var second = stripePaymentService.confirmPayment("pi_test_123");

            assertEquals("FAILED", first.status());
            assertEquals("FAILED", second.status());
            verify(orderService, times(1)).failOrder("MH-20260319-0001");
            verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
        }
    }

    @Test
    @DisplayName("confirmPayment - given missing order number in metadata - throws PaymentException")
    void confirmPayment_givenMissingOrderNumber_throwsPaymentException() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");
        when(mockIntent.getMetadata()).thenReturn(Map.of());

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenReturn(mockIntent);

            assertThrows(PaymentException.class,
                    () -> stripePaymentService.confirmPayment("pi_test_123"));
        }
    }

    @Test
    @DisplayName("confirmPayment - given Stripe error - throws PaymentException")
    void confirmPayment_givenStripeError_throwsPaymentException() {
        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.retrieve("pi_test_123"))
                    .thenThrow(new StripeException("Not found", null, null, 404) {});

            PaymentException exception = assertThrows(PaymentException.class,
                    () -> stripePaymentService.confirmPayment("pi_test_123"));

            assertEquals("Failed to confirm payment: Not found", exception.getMessage());
        }
    }

    // === handleWebhook ===

    @Test
    @DisplayName("handleWebhook - given payment_intent.succeeded event - confirms order")
    void handleWebhook_givenSucceededEvent_confirmsOrder() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_123");
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            stripePaymentService.handleWebhook("{}", "sig_header");

            verify(orderService).confirmOrder("MH-20260319-0001");
        }
    }

    @Test
    @DisplayName("handleWebhook - given payment_intent.payment_failed event - fails order")
    void handleWebhook_givenFailedEvent_failsOrder() {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getMetadata()).thenReturn(Map.of("order_number", "MH-20260319-0001"));

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            stripePaymentService.handleWebhook("{}", "sig_header");

            verify(orderService).failOrder("MH-20260319-0001");
        }
    }

    @Test
    @DisplayName("handleWebhook - given invalid signature - throws PaymentException")
    void handleWebhook_givenInvalidSignature_throwsPaymentException() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("Invalid signature", null));

            assertThrows(PaymentException.class,
                    () -> stripePaymentService.handleWebhook("{}", "bad_sig"));

            verify(orderService, never()).confirmOrder(any());
            verify(orderService, never()).failOrder(any());
        }
    }

    @Test
    @DisplayName("handleWebhook - given unrelated event type - does nothing")
    void handleWebhook_givenUnrelatedEventType_doesNothing() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.created");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            stripePaymentService.handleWebhook("{}", "sig_header");

            verify(orderService, never()).confirmOrder(any());
            verify(orderService, never()).failOrder(any());
        }
    }
}
