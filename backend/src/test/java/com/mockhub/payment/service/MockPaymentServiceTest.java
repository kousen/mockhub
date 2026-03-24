package com.mockhub.payment.service;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.PaymentException;
import com.mockhub.order.entity.Order;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentConfirmation;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.entity.TransactionLog;
import com.mockhub.payment.repository.TransactionLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockPaymentServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private MockPaymentService mockPaymentService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setFirstName("Jane");
        testUser.setLastName("Smith");
        testUser.setRoles(Set.of(buyerRole));

        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setOrderNumber("ORD-20260318-001");
        testOrder.setUser(testUser);
        testOrder.setSubtotal(new BigDecimal("150.00"));
        testOrder.setServiceFee(new BigDecimal("15.00"));
        testOrder.setTotal(new BigDecimal("165.00"));
        testOrder.setStatus("PENDING");
        testOrder.setPaymentMethod("MOCK");
    }

    @Test
    @DisplayName("createPaymentIntent - given valid order - returns PaymentIntentDto with mock values")
    void createPaymentIntent_givenValidOrder_returnsPaymentIntentDtoWithMockValues() {
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentDto result = mockPaymentService.createPaymentIntent(testOrder);

        assertNotNull(result, "PaymentIntentDto should not be null");
        assertTrue(result.paymentIntentId().startsWith("mock_pi_"),
                "Payment intent ID should start with 'mock_pi_'");
        assertTrue(result.clientSecret().startsWith("mock_secret_"),
                "Client secret should start with 'mock_secret_'");
        assertEquals(new BigDecimal("165.00"), result.amount(),
                "Amount should match the order total");
        assertEquals("USD", result.currency(),
                "Currency should be USD");
    }

    @Test
    @DisplayName("createPaymentIntent - given valid order - sets paymentIntentId on order")
    void createPaymentIntent_givenValidOrder_setsPaymentIntentIdOnOrder() {
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentDto result = mockPaymentService.createPaymentIntent(testOrder);

        assertEquals(result.paymentIntentId(), testOrder.getPaymentIntentId(),
                "Order should have the payment intent ID set");
    }

    @Test
    @DisplayName("createPaymentIntent - given valid order - saves transaction log")
    void createPaymentIntent_givenValidOrder_savesTransactionLog() {
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockPaymentService.createPaymentIntent(testOrder);

        verify(transactionLogRepository).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("confirmPayment - given valid payment intent ID - confirms order and returns confirmation")
    void confirmPayment_givenValidPaymentIntentId_confirmsOrderAndReturnsConfirmation() {
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // First create a payment intent so the ID is in the in-memory store
        PaymentIntentDto intent = mockPaymentService.createPaymentIntent(testOrder);
        String paymentIntentId = intent.paymentIntentId();

        when(orderService.getOrderEntityForUpdate("ORD-20260318-001")).thenReturn(testOrder);

        PaymentConfirmation confirmation = mockPaymentService.confirmPayment(paymentIntentId);

        assertNotNull(confirmation, "PaymentConfirmation should not be null");
        assertEquals(paymentIntentId, confirmation.paymentIntentId(),
                "Confirmation should contain the payment intent ID");
        assertEquals("SUCCEEDED", confirmation.status(),
                "Confirmation status should be SUCCEEDED");
        assertEquals("ORD-20260318-001", confirmation.orderNumber(),
                "Confirmation should contain the order number");
        verify(orderService).confirmOrder("ORD-20260318-001");
    }

    @Test
    @DisplayName("confirmPayment - duplicate confirmation returns success without reprocessing")
    void confirmPayment_givenDuplicatePaymentIntent_returnsSuccessWithoutReprocessing() {
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentDto intent = mockPaymentService.createPaymentIntent(testOrder);
        String paymentIntentId = intent.paymentIntentId();

        Order confirmedOrder = new Order();
        confirmedOrder.setId(100L);
        confirmedOrder.setOrderNumber("ORD-20260318-001");
        confirmedOrder.setUser(testUser);
        confirmedOrder.setSubtotal(new BigDecimal("150.00"));
        confirmedOrder.setServiceFee(new BigDecimal("15.00"));
        confirmedOrder.setTotal(new BigDecimal("165.00"));
        confirmedOrder.setStatus("CONFIRMED");
        confirmedOrder.setPaymentMethod("MOCK");

        when(orderService.getOrderEntityForUpdate("ORD-20260318-001")).thenReturn(testOrder);
        when(orderService.getOrderEntityByPaymentIntentIdForUpdate(paymentIntentId)).thenReturn(confirmedOrder);

        PaymentConfirmation first = mockPaymentService.confirmPayment(paymentIntentId);
        PaymentConfirmation second = mockPaymentService.confirmPayment(paymentIntentId);

        assertEquals("SUCCEEDED", first.status());
        assertEquals("SUCCEEDED", second.status());
        verify(orderService).confirmOrder("ORD-20260318-001");
        verify(transactionLogRepository, org.mockito.Mockito.times(2)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("confirmPayment - given unknown payment intent ID - throws PaymentException")
    void confirmPayment_givenUnknownPaymentIntentId_throwsPaymentException() {
        PaymentException exception = assertThrows(PaymentException.class,
                () -> mockPaymentService.confirmPayment("mock_pi_nonexistent"),
                "Should throw PaymentException for unknown payment intent ID");

        assertTrue(exception.getMessage().contains("Unknown payment intent"),
                "Exception message should indicate unknown payment intent");
    }

    @Test
    @DisplayName("confirmPayment - given unknown payment intent ID - does not confirm any order")
    void confirmPayment_givenUnknownPaymentIntentId_doesNotConfirmAnyOrder() {
        assertThrows(PaymentException.class,
                () -> mockPaymentService.confirmPayment("mock_pi_nonexistent"));

        verify(orderService, never()).confirmOrder(any());
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("handleWebhook - given any payload - does nothing (no-op)")
    void handleWebhook_givenAnyPayload_doesNothing() {
        mockPaymentService.handleWebhook("{\"type\":\"test\"}", "sig_header_value");

        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
        verify(orderService, never()).confirmOrder(any());
    }
}
