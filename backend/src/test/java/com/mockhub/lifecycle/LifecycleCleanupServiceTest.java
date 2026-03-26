package com.mockhub.lifecycle;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
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
        when(listingRepository.expireListingsPastDeadline(any(Instant.class))).thenReturn(3);
        when(listingRepository.expireListingsForPastEvents(any(Instant.class))).thenReturn(5);
        when(eventRepository.markPastEventsAsCompleted(any(Instant.class))).thenReturn(2);
        when(notificationRepository.deleteReadNotificationsOlderThan(any(Instant.class))).thenReturn(10);

        cleanupService.runCleanup();

        verify(listingRepository).expireListingsPastDeadline(any(Instant.class));
        verify(listingRepository).expireListingsForPastEvents(any(Instant.class));
        verify(eventRepository).markPastEventsAsCompleted(any(Instant.class));
        verify(notificationRepository).deleteReadNotificationsOlderThan(any(Instant.class));
    }

    @Test
    @DisplayName("expireListingsPastDeadline - returns count of expired listings")
    void expireListingsPastDeadline_returnsExpiredCount() {
        Instant now = Instant.now();
        when(listingRepository.expireListingsPastDeadline(now)).thenReturn(7);

        int result = cleanupService.expireListingsPastDeadline(now);

        assertEquals(7, result);
    }

    @Test
    @DisplayName("expireListingsForPastEvents - returns count of expired listings")
    void expireListingsForPastEvents_returnsExpiredCount() {
        Instant now = Instant.now();
        when(listingRepository.expireListingsForPastEvents(now)).thenReturn(12);

        int result = cleanupService.expireListingsForPastEvents(now);

        assertEquals(12, result);
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
    @DisplayName("runCleanup - given nothing to clean - runs silently without logging")
    void runCleanup_givenNothingToClean_runsSilently() {
        when(listingRepository.expireListingsPastDeadline(any(Instant.class))).thenReturn(0);
        when(listingRepository.expireListingsForPastEvents(any(Instant.class))).thenReturn(0);
        when(eventRepository.markPastEventsAsCompleted(any(Instant.class))).thenReturn(0);
        when(notificationRepository.deleteReadNotificationsOlderThan(any(Instant.class))).thenReturn(0);

        cleanupService.runCleanup();

        verify(listingRepository).expireListingsPastDeadline(any(Instant.class));
        verify(listingRepository).expireListingsForPastEvents(any(Instant.class));
        verify(eventRepository).markPastEventsAsCompleted(any(Instant.class));
        verify(notificationRepository).deleteReadNotificationsOlderThan(any(Instant.class));
    }
}
