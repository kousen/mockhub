package com.mockhub.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleCleanupServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PaymentCredentialRepository paymentCredentialRepository;

    @Mock
    private AgentPurchaseApprovalRepository approvalRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private LifecycleCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new LifecycleCleanupService(
                listingRepository, eventRepository, notificationRepository,
                paymentCredentialRepository, approvalRepository, jdbcTemplate);
    }

    @Test
    @DisplayName("runCleanup - invokes all four cleanup operations")
    void runCleanup_invokesAllFourCleanupOperations() {
        when(listingRepository.findActiveListingsPastDeadline(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(listingRepository.findActiveListingsForPastEvents(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(listingRepository.findActiveListingsForInactiveEvents())
                .thenReturn(Collections.emptyList());
        when(eventRepository.markPastEventsAsCompleted(any(Instant.class))).thenReturn(0);
        when(notificationRepository.deleteReadNotificationsOlderThan(any(Instant.class))).thenReturn(0);
        when(paymentCredentialRepository.expireActiveCredentials(
                any(Instant.class), any(PaymentCredentialStatus.class), any(PaymentCredentialStatus.class)))
                .thenReturn(0);
        when(approvalRepository.expireProposedApprovals(
                any(Instant.class), any(AgentPurchaseApprovalStatus.class), any(AgentPurchaseApprovalStatus.class)))
                .thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(Timestamp.class))).thenReturn(0);

        cleanupService.runCleanup();

        verify(listingRepository).findActiveListingsPastDeadline(any(Instant.class));
        verify(listingRepository).findActiveListingsForPastEvents(any(Instant.class));
        verify(listingRepository).findActiveListingsForInactiveEvents();
        verify(eventRepository).markPastEventsAsCompleted(any(Instant.class));
        verify(notificationRepository).deleteReadNotificationsOlderThan(any(Instant.class));
        verify(paymentCredentialRepository).expireActiveCredentials(
                any(Instant.class), any(PaymentCredentialStatus.class), any(PaymentCredentialStatus.class));
        verify(approvalRepository).expireProposedApprovals(
                any(Instant.class), any(AgentPurchaseApprovalStatus.class), any(AgentPurchaseApprovalStatus.class));
        verify(jdbcTemplate, times(2)).update(anyString(), any(Timestamp.class));
    }

    @Test
    @DisplayName("expireListingsPastDeadline - expires listings and releases tickets")
    void expireListingsPastDeadline_expiresListingsAndReleasesTickets() {
        Instant now = Instant.now();
        Listing listing = createActiveListingWithTicket();
        when(listingRepository.findActiveListingsPastDeadline(now)).thenReturn(List.of(listing));

        int result = cleanupService.expireListingsPastDeadline(now);

        assertEquals(1, result);
        assertEquals("EXPIRED", listing.getStatus());
        assertEquals("AVAILABLE", listing.getTicket().getStatus());
    }

    @Test
    @DisplayName("expireListingsForPastEvents - expires listings and releases tickets")
    void expireListingsForPastEvents_expiresListingsAndReleasesTickets() {
        Instant now = Instant.now();
        Listing listing = createActiveListingWithTicket();
        when(listingRepository.findActiveListingsForPastEvents(now)).thenReturn(List.of(listing));

        int result = cleanupService.expireListingsForPastEvents(now);

        assertEquals(1, result);
        assertEquals("EXPIRED", listing.getStatus());
        assertEquals("AVAILABLE", listing.getTicket().getStatus());
    }

    @Test
    @DisplayName("expireListingsForInactiveEvents - expires listings and releases tickets")
    void expireListingsForInactiveEvents_expiresListingsAndReleasesTickets() {
        Listing listing = createActiveListingWithTicket();
        when(listingRepository.findActiveListingsForInactiveEvents()).thenReturn(List.of(listing));

        int result = cleanupService.expireListingsForInactiveEvents();

        assertEquals(1, result);
        assertEquals("EXPIRED", listing.getStatus());
        assertEquals("AVAILABLE", listing.getTicket().getStatus());
    }

    @Test
    @DisplayName("markPastEventsAsCompleted - returns count of completed events")
    void markPastEventsAsCompleted_returnsCompletedCount() {
        Instant now = Instant.now();
        when(eventRepository.markPastEventsAsCompleted(now)).thenReturn(4);

        int result = cleanupService.markPastEventsAsCompleted(now);

        assertEquals(4, result);
    }

    @Test
    @DisplayName("deleteOldReadNotifications - uses 30-day retention cutoff")
    void deleteOldReadNotifications_uses30DayRetentionCutoff() {
        Instant now = Instant.now();
        Instant expectedCutoff = now.minus(30, ChronoUnit.DAYS);
        when(notificationRepository.deleteReadNotificationsOlderThan(any(Instant.class))).thenReturn(15);

        int result = cleanupService.deleteOldReadNotifications(now);

        assertEquals(15, result);
        verify(notificationRepository).deleteReadNotificationsOlderThan(expectedCutoff);
    }

    @Test
    @DisplayName("expirePaymentCredentials - expires active credentials past expiration")
    void expirePaymentCredentials_expiresActiveCredentialsPastExpiration() {
        Instant now = Instant.now();
        when(paymentCredentialRepository.expireActiveCredentials(
                now, PaymentCredentialStatus.ACTIVE, PaymentCredentialStatus.EXPIRED))
                .thenReturn(3);

        int result = cleanupService.expirePaymentCredentials(now);

        assertEquals(3, result);
    }

    @Test
    @DisplayName("expireProposedApprovals - transitions expired proposed approvals to EXPIRED")
    void expireProposedApprovals_transitionsExpiredProposedApprovals() {
        Instant now = Instant.now();
        when(approvalRepository.expireProposedApprovals(
                now, AgentPurchaseApprovalStatus.PROPOSED, AgentPurchaseApprovalStatus.EXPIRED))
                .thenReturn(2);

        int result = cleanupService.expireProposedApprovals(now);

        assertEquals(2, result);
    }

    @Test
    @DisplayName("deleteExpiredOAuth2Authorizations - deletes rows whose latest token expiry has passed")
    void deleteExpiredOAuth2Authorizations_deletesRowsPastLatestExpiry() {
        Instant now = Instant.now();
        when(jdbcTemplate.update(anyString(), any(Timestamp.class))).thenReturn(7);

        int result = cleanupService.deleteExpiredOAuth2Authorizations(now);

        assertEquals(7, result);
        verify(jdbcTemplate).update(contains("DELETE FROM oauth2_authorization"),
                eq(Timestamp.from(now)));
    }

    @Test
    @DisplayName("deleteStaleUnauthorizedOAuth2Clients - uses 30-day cutoff and spares Claude client")
    void deleteStaleUnauthorizedOAuth2Clients_uses30DayCutoff() {
        Instant now = Instant.now();
        Instant expectedCutoff = now.minus(30, ChronoUnit.DAYS);
        when(jdbcTemplate.update(anyString(), any(Timestamp.class))).thenReturn(2);

        int result = cleanupService.deleteStaleUnauthorizedOAuth2Clients(now);

        assertEquals(2, result);
        verify(jdbcTemplate).update(contains("DELETE FROM oauth2_registered_client"),
                eq(Timestamp.from(expectedCutoff)));
    }

    @Test
    @DisplayName("expireListingsPastDeadline - given no expired listings - returns zero")
    void expireListingsPastDeadline_givenNone_returnsZero() {
        Instant now = Instant.now();
        when(listingRepository.findActiveListingsPastDeadline(now)).thenReturn(Collections.emptyList());

        int result = cleanupService.expireListingsPastDeadline(now);

        assertEquals(0, result);
    }

    private Listing createActiveListingWithTicket() {
        Ticket ticket = new Ticket();
        ticket.setStatus("LISTED");

        Listing listing = new Listing();
        listing.setStatus("ACTIVE");
        listing.setTicket(ticket);
        return listing;
    }
}
