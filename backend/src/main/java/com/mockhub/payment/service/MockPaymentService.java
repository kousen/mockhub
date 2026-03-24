package com.mockhub.payment.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.PaymentException;
import com.mockhub.order.entity.Order;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentConfirmation;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.entity.TransactionLog;
import com.mockhub.payment.repository.TransactionLogRepository;

@Service
@Profile("mock-payment")
public class MockPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentService.class);

    private final TransactionLogRepository transactionLogRepository;
    private final OrderService orderService;

    // In-memory store of mock payment intents mapped to order numbers
    private final ConcurrentHashMap<String, String> paymentIntentToOrder = new ConcurrentHashMap<>();

    public MockPaymentService(TransactionLogRepository transactionLogRepository,
                              OrderService orderService) {
        this.transactionLogRepository = transactionLogRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public PaymentIntentDto createPaymentIntent(Order order) {
        String paymentIntentId = "mock_pi_" + UUID.randomUUID();
        String clientSecret = "mock_secret_" + UUID.randomUUID();

        paymentIntentToOrder.put(paymentIntentId, order.getOrderNumber());

        // Update order with payment intent ID
        order.setPaymentIntentId(paymentIntentId);

        // Log the transaction
        TransactionLog txnLog = new TransactionLog();
        txnLog.setOrder(order);
        txnLog.setUser(order.getUser());
        txnLog.setTransactionType("PAYMENT_INITIATED");
        txnLog.setAmount(order.getTotal());
        txnLog.setCurrency("USD");
        txnLog.setProvider("MOCK");
        txnLog.setProviderReference(paymentIntentId);
        txnLog.setStatus("INITIATED");
        transactionLogRepository.save(txnLog);

        log.info("Created mock payment intent {} for order {}", paymentIntentId, order.getOrderNumber());

        return new PaymentIntentDto(
                paymentIntentId,
                clientSecret,
                order.getTotal(),
                "USD"
        );
    }

    @Override
    @Transactional
    public PaymentConfirmation confirmPayment(String paymentIntentId) {
        Order order = resolveOrderForUpdate(paymentIntentId);
        String orderNumber = order.getOrderNumber();

        if ("CONFIRMED".equals(order.getStatus())) {
            paymentIntentToOrder.remove(paymentIntentId);
            log.info("Mock payment {} already confirmed for order {}", paymentIntentId, orderNumber);
            return new PaymentConfirmation(paymentIntentId, "SUCCEEDED", orderNumber);
        }

        if ("FAILED".equals(order.getStatus())) {
            throw new PaymentException("Cannot confirm payment for failed order " + orderNumber);
        }

        // Simulate processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment processing interrupted");
        }

        // Log successful payment
        TransactionLog txnLog = new TransactionLog();
        txnLog.setOrder(order);
        txnLog.setUser(order.getUser());
        txnLog.setTransactionType("PAYMENT_SUCCEEDED");
        txnLog.setAmount(order.getTotal());
        txnLog.setCurrency("USD");
        txnLog.setProvider("MOCK");
        txnLog.setProviderReference(paymentIntentId);
        txnLog.setStatus("SUCCEEDED");
        transactionLogRepository.save(txnLog);

        // Confirm the order
        orderService.confirmOrder(orderNumber);

        paymentIntentToOrder.remove(paymentIntentId);
        log.info("Confirmed mock payment {} for order {}", paymentIntentId, orderNumber);

        return new PaymentConfirmation(
                paymentIntentId,
                "SUCCEEDED",
                orderNumber
        );
    }

    private Order resolveOrderForUpdate(String paymentIntentId) {
        String orderNumber = paymentIntentToOrder.get(paymentIntentId);
        Order order;
        if (orderNumber != null) {
            order = orderService.getOrderEntityForUpdate(orderNumber);
        } else {
            order = orderService.getOrderEntityByPaymentIntentIdForUpdate(paymentIntentId);
        }

        if (order == null) {
            throw new PaymentException("Unknown payment intent: " + paymentIntentId);
        }

        return order;
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) {
        // No-op for mock payment service
        log.debug("Mock webhook received (no-op)");
    }
}
