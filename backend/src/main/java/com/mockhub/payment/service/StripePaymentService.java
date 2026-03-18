package com.mockhub.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

@Service
@Profile("stripe")
public class StripePaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    private static final String METADATA_ORDER_NUMBER = "order_number";
    private static final String PROVIDER = "STRIPE";
    private static final String CURRENCY = "USD";

    private final String webhookSecret;
    private final TransactionLogRepository transactionLogRepository;
    private final OrderService orderService;

    public StripePaymentService(@Value("${stripe.secret-key}") String secretKey,
                                @Value("${stripe.webhook-secret}") String webhookSecret,
                                TransactionLogRepository transactionLogRepository,
                                OrderService orderService) {
        this.webhookSecret = webhookSecret;
        this.transactionLogRepository = transactionLogRepository;
        this.orderService = orderService;
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public PaymentIntentDto createPaymentIntent(Order order) {
        try {
            // Stripe amount is in cents
            long amountInCents = order.getTotal().movePointRight(2).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata(METADATA_ORDER_NUMBER, order.getOrderNumber())
                    .putMetadata("user_id", order.getUser().getId().toString())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Update order with payment intent ID
            order.setPaymentIntentId(paymentIntent.getId());

            // Log the transaction
            TransactionLog txnLog = new TransactionLog();
            txnLog.setOrder(order);
            txnLog.setUser(order.getUser());
            txnLog.setTransactionType("PAYMENT_INITIATED");
            txnLog.setAmount(order.getTotal());
            txnLog.setCurrency(CURRENCY);
            txnLog.setProvider(PROVIDER);
            txnLog.setProviderReference(paymentIntent.getId());
            txnLog.setStatus("INITIATED");
            transactionLogRepository.save(txnLog);

            log.info("Created Stripe PaymentIntent {} for order {}",
                    paymentIntent.getId(), order.getOrderNumber());

            return new PaymentIntentDto(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    order.getTotal(),
                    CURRENCY
            );
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent for order {}", order.getOrderNumber(), e);
            throw new PaymentException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentConfirmation confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String status = paymentIntent.getStatus();
            String orderNumber = paymentIntent.getMetadata().get(METADATA_ORDER_NUMBER);

            if (orderNumber == null) {
                throw new PaymentException("No order number found in payment intent metadata");
            }

            Order order = orderService.getOrderEntity(orderNumber);

            if ("succeeded".equals(status)) {
                TransactionLog txnLog = new TransactionLog();
                txnLog.setOrder(order);
                txnLog.setUser(order.getUser());
                txnLog.setTransactionType("PAYMENT_SUCCEEDED");
                txnLog.setAmount(order.getTotal());
                txnLog.setCurrency(CURRENCY);
                txnLog.setProvider(PROVIDER);
                txnLog.setProviderReference(paymentIntentId);
                txnLog.setStatus("SUCCEEDED");
                transactionLogRepository.save(txnLog);

                orderService.confirmOrder(orderNumber);

                return new PaymentConfirmation(paymentIntentId, "SUCCEEDED", orderNumber);
            } else {
                TransactionLog txnLog = new TransactionLog();
                txnLog.setOrder(order);
                txnLog.setUser(order.getUser());
                txnLog.setTransactionType("PAYMENT_FAILED");
                txnLog.setAmount(order.getTotal());
                txnLog.setCurrency(CURRENCY);
                txnLog.setProvider(PROVIDER);
                txnLog.setProviderReference(paymentIntentId);
                txnLog.setStatus("FAILED");
                transactionLogRepository.save(txnLog);

                orderService.failOrder(orderNumber);

                return new PaymentConfirmation(paymentIntentId, "FAILED", orderNumber);
            }
        } catch (StripeException e) {
            log.error("Failed to confirm Stripe payment {}", paymentIntentId, e);
            throw new PaymentException("Failed to confirm payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            throw new PaymentException("Invalid webhook signature");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook event: {}", eventType);

        if ("payment_intent.succeeded".equals(eventType)) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (paymentIntent != null) {
                String orderNumber = paymentIntent.getMetadata().get(METADATA_ORDER_NUMBER);
                if (orderNumber != null) {
                    orderService.confirmOrder(orderNumber);
                    log.info("Webhook confirmed order {} via payment intent {}",
                            orderNumber, paymentIntent.getId());
                }
            }
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (paymentIntent != null) {
                String orderNumber = paymentIntent.getMetadata().get(METADATA_ORDER_NUMBER);
                if (orderNumber != null) {
                    orderService.failOrder(orderNumber);
                    log.info("Webhook failed order {} via payment intent {}",
                            orderNumber, paymentIntent.getId());
                }
            }
        }
    }
}
