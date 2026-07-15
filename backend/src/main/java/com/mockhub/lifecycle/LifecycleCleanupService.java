package com.mockhub.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

@Service
public class LifecycleCleanupService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleCleanupService.class);
    private static final int NOTIFICATION_RETENTION_DAYS = 30;
    private static final int DCR_CLIENT_RETENTION_DAYS = 30;
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String TICKET_AVAILABLE = "AVAILABLE";

    private final ListingRepository listingRepository;
    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentCredentialRepository paymentCredentialRepository;
    private final AgentPurchaseApprovalRepository approvalRepository;
    private final JdbcTemplate jdbcTemplate;

    public LifecycleCleanupService(ListingRepository listingRepository,
                                   EventRepository eventRepository,
                                   NotificationRepository notificationRepository,
                                   PaymentCredentialRepository paymentCredentialRepository,
                                   AgentPurchaseApprovalRepository approvalRepository,
                                   JdbcTemplate jdbcTemplate) {
        this.listingRepository = listingRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.paymentCredentialRepository = paymentCredentialRepository;
        this.approvalRepository = approvalRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRateString = "${mockhub.lifecycle.cleanup-interval:900000}")
    @Transactional
    public void runCleanup() {
        Instant now = Instant.now();

        int expiredByDeadline = expireListingsPastDeadline(now);
        int expiredByEvent = expireListingsForPastEvents(now);
        int completedEvents = markPastEventsAsCompleted(now);
        int deletedNotifications = deleteOldReadNotifications(now);
        int expiredPaymentCredentials = expirePaymentCredentials(now);
        int expiredApprovals = expireProposedApprovals(now);
        int deletedOAuth2Authorizations = deleteExpiredOAuth2Authorizations(now);
        int deletedOAuth2Clients = deleteStaleUnauthorizedOAuth2Clients(now);

        if (expiredByDeadline + expiredByEvent + completedEvents + deletedNotifications
                + expiredPaymentCredentials + expiredApprovals + deletedOAuth2Authorizations
                + deletedOAuth2Clients > 0) {
            log.info("Lifecycle cleanup: expired {} listings (deadline), {} listings (past events), "
                    + "completed {} events, deleted {} old notifications, expired {} payment credentials, "
                    + "expired {} purchase approvals, deleted {} expired OAuth2 authorizations, "
                    + "deleted {} stale OAuth2 clients",
                    expiredByDeadline, expiredByEvent, completedEvents, deletedNotifications,
                    expiredPaymentCredentials, expiredApprovals, deletedOAuth2Authorizations,
                    deletedOAuth2Clients);
        }
    }

    int expireListingsPastDeadline(Instant now) {
        List<Listing> listings = listingRepository.findActiveListingsPastDeadline(now);
        expireListingsAndReleaseTickets(listings);
        return listings.size();
    }

    int expireListingsForPastEvents(Instant now) {
        List<Listing> listings = listingRepository.findActiveListingsForPastEvents(now);
        expireListingsAndReleaseTickets(listings);
        return listings.size();
    }

    int markPastEventsAsCompleted(Instant now) {
        return eventRepository.markPastEventsAsCompleted(now);
    }

    int deleteOldReadNotifications(Instant now) {
        Instant cutoff = now.minus(NOTIFICATION_RETENTION_DAYS, ChronoUnit.DAYS);
        return notificationRepository.deleteReadNotificationsOlderThan(cutoff);
    }

    int expirePaymentCredentials(Instant now) {
        return paymentCredentialRepository.expireActiveCredentials(
                now, PaymentCredentialStatus.ACTIVE, PaymentCredentialStatus.EXPIRED);
    }

    int expireProposedApprovals(Instant now) {
        return approvalRepository.expireProposedApprovals(
                now, AgentPurchaseApprovalStatus.PROPOSED, AgentPurchaseApprovalStatus.EXPIRED);
    }

    /**
     * Deletes MCP OAuth2 authorizations whose latest token expiry has passed, so the
     * table doesn't grow forever now that authorizations are persisted (issue #266).
     *
     * <p>Postgres {@code GREATEST} ignores NULLs, so a row is deleted once every token
     * it actually has (across all grant types the schema supports) is expired.
     * Rows with no expiries at all (abandoned in-flight authorize requests) are left
     * alone — they carry no tokens and are replaced on the next login attempt.</p>
     */
    int deleteExpiredOAuth2Authorizations(Instant now) {
        return jdbcTemplate.update("""
                DELETE FROM oauth2_authorization
                WHERE GREATEST(authorization_code_expires_at,
                               access_token_expires_at,
                               refresh_token_expires_at,
                               oidc_id_token_expires_at,
                               user_code_expires_at,
                               device_code_expires_at) < ?
                """, Timestamp.from(now));
    }

    /**
     * Deletes DCR-registered OAuth2 clients that were never authorized (or whose
     * authorizations have all been cleaned up) after 30 days. The DCR endpoint is
     * intentionally open, so without this an attacker could accumulate unbounded
     * permanent rows. Clients with any live authorization are kept — an idle-but-valid
     * connector still has its refresh-token authorization row. The pre-registered
     * Claude client is excluded (and re-seeded on startup regardless).
     */
    int deleteStaleUnauthorizedOAuth2Clients(Instant now) {
        Instant cutoff = now.minus(DCR_CLIENT_RETENTION_DAYS, ChronoUnit.DAYS);
        return jdbcTemplate.update("""
                DELETE FROM oauth2_registered_client c
                WHERE c.client_id_issued_at < ?
                  AND c.client_id <> 'claude-mcp-client'
                  AND NOT EXISTS (SELECT 1 FROM oauth2_authorization a
                                  WHERE a.registered_client_id = c.id)
                """, Timestamp.from(cutoff));
    }

    private void expireListingsAndReleaseTickets(List<Listing> listings) {
        for (Listing listing : listings) {
            listing.setStatus(STATUS_EXPIRED);
            listing.getTicket().setStatus(TICKET_AVAILABLE);
        }
    }
}
