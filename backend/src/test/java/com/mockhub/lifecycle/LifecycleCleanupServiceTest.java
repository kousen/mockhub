package com.mockhub.lifecycle;

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

import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    private LifecycleCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new LifecycleCleanupService(
                listingRepository, eventRepository, notificationRepository);
    }

    @Test
    @DisplayName("runCleanup - invokes all four cleanup operations")
    void runCleanup_invokesAllFourCleanupOperations() {
        when(listingRepository.findActiveListingsPastDeadline(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(listingRepository.findActiveListingsForPastEvents(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.markPastEventsAsCompleted(any(Instant.class))).thenReturn(0);
        when(notificationRepository.deleteReadNotificationsOlderThan(any(Instant.class))).thenReturn(0);

        cleanupService.runCleanup();

        verify(listingRepository).findActiveListingsPastDeadline(any(Instant.class));
        verify(listingRepository).findActiveListingsForPastEvents(any(Instant.class));
        verify(eventRepository).markPastEventsAsCompleted(any(Instant.class));
        verify(notificationRepository).deleteReadNotificationsOlderThan(any(Instant.class));
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
