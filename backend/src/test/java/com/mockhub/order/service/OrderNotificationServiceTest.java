package com.mockhub.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.User;
import com.mockhub.event.entity.Event;
import com.mockhub.notification.entity.NotificationType;
import com.mockhub.notification.service.EmailDeliveryService;
import com.mockhub.notification.service.NotificationService;
import com.mockhub.notification.service.SmsDeliveryService;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.venue.entity.Section;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SmsDeliveryService smsDeliveryService;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private TicketSigningService ticketSigningService;

    private OrderNotificationService orderNotificationService;

    private Order testOrder;
    private User testUser;

    @BeforeEach
    void setUp() {
        orderNotificationService = new OrderNotificationService(
                notificationService, smsDeliveryService, emailDeliveryService,
                ticketSigningService, "http://localhost:5173");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setPhone("+15551234567");

        Event testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");

        Section testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Floor");

        Ticket testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setTicketType("GENERAL_ADMISSION");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("SOLD");

        Listing testListing = new Listing();
        testListing.setId(1L);
        testListing.setTicket(testTicket);
        testListing.setEvent(testEvent);
        testListing.setListedPrice(new BigDecimal("75.00"));
        testListing.setComputedPrice(new BigDecimal("75.00"));
        testListing.setPriceMultiplier(BigDecimal.ONE);
        testListing.setStatus("SOLD");
        testListing.setListedAt(Instant.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setListing(testListing);
        orderItem.setTicket(testTicket);
        orderItem.setPricePaid(new BigDecimal("75.00"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setOrderNumber("MH-20260317-0001");
        testOrder.setStatus("CONFIRMED");
        testOrder.setSubtotal(new BigDecimal("75.00"));
        testOrder.setServiceFee(new BigDecimal("7.50"));
        testOrder.setTotal(new BigDecimal("82.50"));
        testOrder.setPaymentMethod("mock");
        testOrder.setCreatedAt(Instant.now());
        testOrder.setItems(List.of(orderItem));
        orderItem.setOrder(testOrder);
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given confirmed order - creates in-app notification")
    void sendConfirmationNotifications_givenConfirmedOrder_createsInAppNotification() {
        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_CONFIRMED),
                eq("Order Confirmed"),
                contains("MH-20260317-0001"),
                eq("/orders/MH-20260317-0001/confirmation"));
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given user with phone - sends SMS")
    void sendConfirmationNotifications_givenUserWithPhone_sendsSms() {
        when(ticketSigningService.generateOrderViewToken("MH-20260317-0001"))
                .thenReturn("test-token");

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(smsDeliveryService).sendSms(
                eq("+15551234567"),
                contains("Test Event"));
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given user without phone - skips SMS")
    void sendConfirmationNotifications_givenUserWithoutPhone_skipsSms() {
        testUser.setPhone(null);

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(smsDeliveryService, never()).sendSms(anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given user with blank phone - skips SMS")
    void sendConfirmationNotifications_givenUserWithBlankPhone_skipsSms() {
        testUser.setPhone("   ");

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(smsDeliveryService, never()).sendSms(anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given user with email - sends email")
    void sendConfirmationNotifications_givenUserWithEmail_sendsEmail() {
        when(ticketSigningService.generateOrderViewToken("MH-20260317-0001"))
                .thenReturn("email-token");

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(emailDeliveryService).sendEmail(
                eq("buyer@example.com"),
                contains("Test Event"),
                contains("MH-20260317-0001"));
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given user without email - skips email")
    void sendConfirmationNotifications_givenUserWithoutEmail_skipsEmail() {
        testUser.setEmail(null);

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(emailDeliveryService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given email delivery failure - does not throw")
    void sendConfirmationNotifications_givenEmailDeliveryFailure_doesNotThrow() {
        when(ticketSigningService.generateOrderViewToken("MH-20260317-0001"))
                .thenReturn("test-token");
        when(emailDeliveryService.sendEmail(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("SMTP connection refused"));

        // Should not throw - email failures are caught and logged
        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(notificationService).createNotification(
                anyLong(), eq(NotificationType.ORDER_CONFIRMED),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given SMS token - includes ticket view URL")
    void sendConfirmationNotifications_givenSmsToken_includesTicketViewUrl() {
        when(ticketSigningService.generateOrderViewToken("MH-20260317-0001"))
                .thenReturn("sms-view-token");

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(smsDeliveryService).sendSms(
                eq("+15551234567"),
                contains("http://localhost:5173/tickets/view?token=sms-view-token"));
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given email token - includes ticket view URL in HTML")
    void sendConfirmationNotifications_givenEmailToken_includesTicketViewUrlInHtml() {
        when(ticketSigningService.generateOrderViewToken("MH-20260317-0001"))
                .thenReturn("email-view-token");

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(emailDeliveryService).sendEmail(
                eq("buyer@example.com"),
                anyString(),
                contains("http://localhost:5173/tickets/view?token=email-view-token"));
    }

    @Test
    @DisplayName("sendConfirmationNotifications - given order with no items - uses fallback event name")
    void sendConfirmationNotifications_givenOrderWithNoItems_usesFallbackEventName() {
        testOrder.setItems(List.of());

        orderNotificationService.sendConfirmationNotifications(testOrder);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_CONFIRMED),
                eq("Order Confirmed"),
                contains("82.50"),
                anyString());
    }
}
