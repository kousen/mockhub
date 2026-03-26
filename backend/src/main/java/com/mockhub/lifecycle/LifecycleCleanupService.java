package com.mockhub.lifecycle;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
import com.mockhub.ticket.repository.ListingRepository;

@Service
public class LifecycleCleanupService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleCleanupService.class);
    private static final int NOTIFICATION_RETENTION_DAYS = 30;

    private final ListingRepository listingRepository;
    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;

    public LifecycleCleanupService(ListingRepository listingRepository,
                                   EventRepository eventRepository,
                                   NotificationRepository notificationRepository) {
        this.listingRepository = listingRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(fixedRateString = "${mockhub.lifecycle.cleanup-interval:900000}")
    @Transactional
    public void runCleanup() {
        Instant now = Instant.now();

        int expiredByDeadline = expireListingsPastDeadline(now);
        int expiredByEvent = expireListingsForPastEvents(now);
        int completedEvents = markPastEventsAsCompleted(now);
        int deletedNotifications = deleteOldReadNotifications(now);

        if (expiredByDeadline + expiredByEvent + completedEvents + deletedNotifications > 0) {
            log.info("Lifecycle cleanup: expired {} listings (deadline), {} listings (past events), "
                    + "completed {} events, deleted {} old notifications",
                    expiredByDeadline, expiredByEvent, completedEvents, deletedNotifications);
        }
    }

    int expireListingsPastDeadline(Instant now) {
        return listingRepository.expireListingsPastDeadline(now);
    }

    int expireListingsForPastEvents(Instant now) {
        return listingRepository.expireListingsForPastEvents(now);
    }

    int markPastEventsAsCompleted(Instant now) {
        return eventRepository.markPastEventsAsCompleted(now);
    }

    int deleteOldReadNotifications(Instant now) {
        Instant cutoff = now.minus(NOTIFICATION_RETENTION_DAYS, ChronoUnit.DAYS);
        return notificationRepository.deleteReadNotificationsOlderThan(cutoff);
    }
}
