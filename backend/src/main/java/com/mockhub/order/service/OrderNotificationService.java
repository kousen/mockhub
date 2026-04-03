package com.mockhub.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.service.EmailDeliveryService;
import com.mockhub.notification.service.NotificationService;
import com.mockhub.notification.service.SmsDeliveryService;
import com.mockhub.order.entity.Order;
import com.mockhub.ticket.service.TicketSigningService;

/**
 * Handles all confirmation notifications for completed orders:
 * in-app notifications, SMS, and email delivery.
 *
 * Extracted from OrderService to honor the Single Responsibility Principle —
 * OrderService owns order lifecycle; this service owns notification dispatch.
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private final NotificationService notificationService;
    private final SmsDeliveryService smsDeliveryService;
    private final EmailDeliveryService emailDeliveryService;
    private final TicketSigningService ticketSigningService;
    private final String orderBaseUrl;

    public OrderNotificationService(NotificationService notificationService,
                                    SmsDeliveryService smsDeliveryService,
                                    EmailDeliveryService emailDeliveryService,
                                    TicketSigningService ticketSigningService,
                                    @Value("${mockhub.sms.order-base-url}") String orderBaseUrl) {
        this.notificationService = notificationService;
        this.smsDeliveryService = smsDeliveryService;
        this.emailDeliveryService = emailDeliveryService;
        this.ticketSigningService = ticketSigningService;
        this.orderBaseUrl = orderBaseUrl;
    }

    /**
     * Sends all confirmation notifications for a confirmed order:
     * in-app notification, SMS (if phone available), and email (if email available).
     */
    public void sendConfirmationNotifications(Order order) {
        String orderNumber = order.getOrderNumber();

        notificationService.createNotification(
                order.getUser().getId(),
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                String.format("Your order %s has been confirmed. Total: $%s",
                        orderNumber, order.getTotal().toPlainString()),
                "/orders/" + orderNumber + "/confirmation"
        );

        sendSmsConfirmation(order);
        sendEmailConfirmation(order);
    }

    private void sendSmsConfirmation(Order order) {
        String phone = order.getUser().getPhone();
        if (phone == null || phone.isBlank()) {
            return;
        }

        String eventName = order.getItems().stream()
                .findFirst()
                .map(item -> item.getListing().getEvent().getName())
                .orElse("your event");
        String orderViewToken = ticketSigningService.generateOrderViewToken(order.getOrderNumber());
        String orderUrl = orderBaseUrl + "/tickets/view?token=" + orderViewToken;
        String smsMessage = String.format(
                "MockHub: Your tickets for %s are confirmed! View your tickets: %s",
                eventName, orderUrl);
        smsDeliveryService.sendSms(phone, smsMessage);
    }

    private void sendEmailConfirmation(Order order) {
        String email = order.getUser().getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            String eventName = order.getItems().stream()
                    .findFirst()
                    .map(item -> item.getListing().getEvent().getName())
                    .orElse("your event");
            String emailToken = ticketSigningService.generateOrderViewToken(order.getOrderNumber());
            String ticketUrl = orderBaseUrl + "/tickets/view?token=" + emailToken;

            String htmlBody = buildConfirmationEmail(order.getOrderNumber(), eventName,
                    order.getTotal().toPlainString(), order.getItems().size(), ticketUrl);
            emailDeliveryService.sendEmail(email,
                    "Your MockHub tickets for " + eventName, htmlBody);
        } catch (Exception exception) {
            log.error("Failed to send confirmation email for order {}: {}",
                    order.getOrderNumber(), exception.getMessage());
        }
    }

    private String buildConfirmationEmail(String orderNumber, String eventName,
                                           String total, int ticketCount, String ticketUrl) {
        return String.format("""
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; \
                max-width: 600px; margin: 0 auto; padding: 24px;">
                  <h1 style="font-size: 24px; margin-bottom: 8px;">Your tickets are confirmed!</h1>
                  <p style="color: #666; margin-bottom: 24px;">Order %s</p>
                  <div style="background: #f9fafb; border-radius: 8px; padding: 20px; margin-bottom: 24px;">
                    <h2 style="font-size: 18px; margin: 0 0 8px 0;">%s</h2>
                    <p style="color: #666; margin: 0;">%d ticket%s &middot; Total: $%s</p>
                  </div>
                  <a href="%s" style="display: inline-block; background: #18181b; color: #fff; \
                padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 500;">
                    View Your Tickets
                  </a>
                  <p style="color: #999; font-size: 12px; margin-top: 24px;">
                    Tap the button above to see your scannable QR code tickets. No login required.
                  </p>
                </div>
                """, orderNumber, eventName, ticketCount,
                ticketCount == 1 ? "" : "s", total, ticketUrl);
    }
}
