package com.mockhub.payment.service;

import com.mockhub.order.entity.Order;
import com.mockhub.payment.dto.PaymentConfirmation;
import com.mockhub.payment.dto.PaymentIntentDto;

public interface PaymentService {

    PaymentIntentDto createPaymentIntent(Order order);

    PaymentConfirmation confirmPayment(String paymentIntentId);

    void handleWebhook(String payload, String sigHeader);
}
