package com.mockhub.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.notification.repository.NotificationRepository;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;

@Service
public class LifecycleCleanupService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleCleanupService.class);
    private static final int NOTIFICATION_RETENTION_DAYS = 30;
    private static final int DCR_CLIENT_RETENTION_DAYS = 30;
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final int ABANDONED_CHECKOUT_BATCH_SIZE = 500;
    private static final String TICKET_AVAILABLE = "AVAILABLE";

    private final ListingRepository listingRepository;
    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentCredentialRepository paymentCredentialRepository;
    private final AgentPurchaseApprovalRepository approvalRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final int priceHistoryRetentionDays;
    private final int abandonedCheckoutMinutes;

    public LifecycleCleanupService(ListingRepository listingRepository,
                                   EventRepository eventRepository,
                                   NotificationRepository notificationRepository,
                                   PaymentCredentialRepository paymentCredentialRepository,
                                   AgentPurchaseApprovalRepository approvalRepository,
                                   JdbcTemplate jdbcTemplate,
                                   PriceHistoryRepository priceHistoryRepository,
                                   TicketRepository ticketRepository,
                                   OrderRepository orderRepository,
                                   OrderService orderService,
                                   @Value("${mockhub.pricing.history-retention-days:90}")
                                   int priceHistoryRetentionDays,
                                   @Value("${mockhub.lifecycle.abandoned-checkout-minutes:30}")
                                   int abandonedCheckoutMinutes) {
        this.listingRepository = listingRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.paymentCredentialRepository = paymentCredentialRepository;
        this.approvalRepository = approvalRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.priceHistoryRepository = priceHistoryRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.priceHistoryRetentionDays = priceHistoryRetentionDays;
        this.abandonedCheckoutMinutes = abandonedCheckoutMinutes;
    }

    @Scheduled(fixedRateString = "${mockhub.lifecycle.cleanup-interval:900000}")
    @Transactional
    public void runCleanup() {
        Instant now = Instant.now();

        int expiredByDeadline = expireListingsPastDeadline(now);
        int expiredByEvent = expireListingsForPastEvents(now);
        int expiredByInactiveEvent = expireListingsForInactiveEvents();
        int completedEvents = markPastEventsAsCompleted(now);
        int deletedNotifications = deleteOldReadNotifications(now);
        int expiredPaymentCredentials = expirePaymentCredentials(now);
        int expiredApprovals = expireProposedApprovals(now);
        int abandonedCheckouts = failAbandonedCheckouts(now);
        int deletedOAuth2Authorizations = deleteExpiredOAuth2Authorizations(now);
        int deletedOAuth2Clients = deleteStaleUnauthorizedOAuth2Clients(now);
        int purgedPriceRows = purgeDeadPriceHistory(now);
        int purgedListings = purgeOrphanedListingsForInactiveEvents();
        int purgedTickets = purgeOrphanedTicketsForInactiveEvents();

        if (expiredByDeadline + expiredByEvent + expiredByInactiveEvent + completedEvents
                + deletedNotifications + expiredPaymentCredentials + expiredApprovals
                + abandonedCheckouts + deletedOAuth2Authorizations + deletedOAuth2Clients
                + purgedPriceRows + purgedListings + purgedTickets > 0) {
            log.info("Lifecycle cleanup: expired {} listings (deadline), {} listings (past events), "
                    + "{} listings (cancelled/inactive events), "
                    + "completed {} events, deleted {} old notifications, expired {} payment credentials, "
                    + "expired {} purchase approvals, failed {} abandoned checkouts, "
                    + "deleted {} expired OAuth2 authorizations, "
                    + "deleted {} stale OAuth2 clients, purged {} price snapshots, "
                    + "purged {} orphaned listings, purged {} orphaned tickets",
                    expiredByDeadline, expiredByEvent, expiredByInactiveEvent, completedEvents,
                    deletedNotifications, expiredPaymentCredentials, expiredApprovals,
                    abandonedCheckouts, deletedOAuth2Authorizations, deletedOAuth2Clients,
                    purgedPriceRows, purgedListings, purgedTickets);
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

    /**
     * Cancelled (or otherwise deactivated) future events keep their listings
     * ACTIVE until this runs — e.g. the Ticketmaster sync honoring a "cancelled"
     * status from upstream. Search already filters these out; this releases the
     * inventory so it stops appearing anywhere.
     */
    int expireListingsForInactiveEvents() {
        List<Listing> listings = listingRepository.findActiveListingsForInactiveEvents();
        expireListingsAndReleaseTickets(listings);
        return listings.size();
    }

    /**
     * Dead events don't need price history, and live events don't need it
     * forever (one snapshot per price change; retention window still bounds it).
     * ponytail: single-statement bulk deletes — the first production run removes
     * ~3M rows in one transaction; batch it if that ever times out.
     */
    int purgeDeadPriceHistory(Instant now) {
        int purged = priceHistoryRepository.deleteForInactiveEvents();
        Instant cutoff = now.minus(priceHistoryRetentionDays, ChronoUnit.DAYS);
        return purged + priceHistoryRepository.deleteRecordedBefore(cutoff);
    }

    /**
     * Inventory for dead events is unsellable; keep only rows that order
     * history references. Listings go first so their tickets become orphans.
     */
    int purgeOrphanedListingsForInactiveEvents() {
        return listingRepository.deleteOrphanedForInactiveEvents();
    }

    int purgeOrphanedTicketsForInactiveEvents() {
        return ticketRepository.deleteOrphanedForInactiveEvents();
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
     * Fails checkouts left pending past the abandonment window, releasing their tickets.
     *
     * <p>An agent that creates a checkout and never completes or cancels it holds those
     * seats indefinitely — every later attempt on the same listing fails with "no longer
     * available" even though nobody bought it. Failing the order is the existing path that
     * returns the tickets to inventory.
     */
    int failAbandonedCheckouts(Instant now) {
        Instant cutoff = now.minus(abandonedCheckoutMinutes, ChronoUnit.MINUTES);
        List<Order> abandoned = orderRepository.findAbandonedPendingOrders(
                cutoff, PageRequest.of(0, ABANDONED_CHECKOUT_BATCH_SIZE));
        if (abandoned.size() == ABANDONED_CHECKOUT_BATCH_SIZE) {
            log.warn("Abandoned-checkout sweep hit its batch limit of {}; a backlog is building",
                    ABANDONED_CHECKOUT_BATCH_SIZE);
        }

        int failed = 0;
        for (Order order : abandoned) {
            try {
                // Each order commits on its own. Sharing this sweep's transaction would let
                // one failing order mark it rollback-only and discard every other cleanup
                // step at commit — catching the exception here cannot undo that.
                orderService.failOrderInNewTransaction(order.getOrderNumber());
                failed++;
            } catch (ConflictException e) {
                // The order was confirmed or cancelled between the query and the update.
                log.info("Abandoned checkout {} changed state before cleanup could fail it: {}",
                        order.getOrderNumber(), e.getMessage());
            } catch (RuntimeException e) {
                // Anything else means these seats are still held and cleanup cannot free them.
                log.error("Could not fail abandoned checkout {} — its tickets remain held and "
                        + "stay out of inventory until this is resolved", order.getOrderNumber(), e);
            }
        }
        return failed;
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
