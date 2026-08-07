package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the concurrent-duplicate idempotency race: two checkouts with the same
 * Idempotency-Key can both miss the in-transaction lookup, and the loser's insert
 * dies on the partial unique index. The same-thread retry tests never hit this —
 * only a genuinely concurrent test does.
 *
 * <p>Thread A inserts an order with the key and holds its transaction open; thread B
 * runs a real checkout with the same key, misses the lookup (A is uncommitted), and
 * blocks on the unique index until A commits — then gets the constraint violation.
 * The controller-level recovery ({@code findOrderForIdempotentRetry}) must then
 * return A's order, which is what the caller's retry would have received.</p>
 */
class IdempotencyConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final String IDEMPOTENCY_KEY = "conc-idem-key";

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartService cartService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private User buyer;
    private Listing listing;

    @BeforeEach
    void setUp() {
        Role buyerRole = roleRepository.findByName("ROLE_BUYER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_BUYER")));

        buyer = userRepository.findByEmail("idem-conc@test.com").orElseGet(() -> {
            User user = new User();
            user.setEmail("idem-conc@test.com");
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setFirstName("Idem");
            user.setLastName("Concurrent");
            user.setRoles(Set.of(buyerRole));
            return userRepository.save(user);
        });

        Venue venue = new Venue();
        venue.setName("Idem Venue " + System.nanoTime());
        venue.setSlug("idem-venue-" + System.nanoTime());
        venue.setCity("New York");
        venue.setState("NY");
        venue.setAddressLine1("123 Test St");
        venue.setZipCode("10001");
        venue.setCountry("US");
        venue.setCapacity(100);
        venue.setVenueType("ARENA");

        Section section = new Section();
        section.setVenue(venue);
        section.setName("General Admission");
        section.setSectionType("GENERAL_ADMISSION");
        section.setCapacity(100);
        section.setSortOrder(1);
        venue.getSections().add(section);
        venue = venueRepository.save(venue);
        section = venue.getSections().getFirst();

        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Category cat = new Category();
                    cat.setName("Concerts");
                    cat.setSlug("concerts");
                    return categoryRepository.save(cat);
                });

        Event event = new Event();
        event.setName("Idempotency Test Event " + System.nanoTime());
        event.setSlug("idem-test-" + System.nanoTime());
        event.setDescription("Test event");
        event.setVenue(venue);
        event.setCategory(category);
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setStatus("ACTIVE");
        event.setBasePrice(new BigDecimal("50.00"));
        event.setMinPrice(new BigDecimal("40.00"));
        event.setMaxPrice(new BigDecimal("100.00"));
        event.setTotalTickets(100);
        event.setAvailableTickets(100);
        event = eventRepository.save(event);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setSection(section);
        ticket.setTicketType("GENERAL_ADMISSION");
        ticket.setFaceValue(new BigDecimal("50.00"));
        ticket.setStatus("AVAILABLE");
        ticket.setBarcode("IDEM-TEST-" + System.nanoTime());
        ticket = ticketRepository.save(ticket);

        listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setListedPrice(new BigDecimal("75.00"));
        listing.setComputedPrice(new BigDecimal("75.00"));
        listing.setPriceMultiplier(BigDecimal.ONE);
        listing.setStatus("ACTIVE");
        listing.setListedAt(Instant.now());
        listing.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        listing = listingRepository.save(listing);
    }

    @AfterEach
    void cleanUp() {
        orderRepository.findByIdempotencyKey(IDEMPOTENCY_KEY).ifPresent(order -> {
            order.setIdempotencyKey(null);
            orderRepository.save(order);
        });
    }

    @Test
    @DisplayName("concurrent checkout - same idempotency key - loser recovers the winner's order")
    void concurrentCheckout_sameIdempotencyKey_loserRecoversWinnersOrder() throws Exception {
        cartService.addToCart(buyer, listing.getId());

        CountDownLatch winnerInserted = new CountDownLatch(1);
        CountDownLatch releaseWinner = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            // Thread A: insert an order with the key, then hold the transaction open
            Future<?> winner = executor.submit(() -> txTemplate.executeWithoutResult(status -> {
                Order order = new Order();
                order.setUser(buyer);
                order.setOrderNumber("MH-IDEM-9999");
                order.setStatus(OrderStatus.PENDING);
                order.setSubtotal(new BigDecimal("75.00"));
                order.setServiceFee(new BigDecimal("7.50"));
                order.setTotal(new BigDecimal("82.50"));
                order.setPaymentMethod("MOCK");
                order.setIdempotencyKey(IDEMPOTENCY_KEY);
                orderRepository.saveAndFlush(order);
                winnerInserted.countDown();
                try {
                    // Hold the row's index entry until the loser is blocked on it
                    if (!releaseWinner.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Winner was never released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));

            assertTrue(winnerInserted.await(15, TimeUnit.SECONDS), "Winner should insert first");

            // Thread B: full checkout with the same key. The lookup misses (A is
            // uncommitted); the insert blocks on the unique index until A commits.
            Future<Throwable> loser = executor.submit(() -> {
                try {
                    orderService.checkout(buyer, new CheckoutRequest("mock"), IDEMPOTENCY_KEY);
                    return null;
                } catch (RuntimeException ex) {
                    return ex;
                }
            });

            // Give the loser time to pass the lookup and block on the index insert,
            // then let the winner commit
            Thread.sleep(1000);
            releaseWinner.countDown();
            winner.get(15, TimeUnit.SECONDS);

            Throwable thrown = loser.get(15, TimeUnit.SECONDS);
            assertNotNull(thrown, "Loser should fail on the unique index, not succeed");
            assertInstanceOf(DataIntegrityViolationException.class, thrown,
                    "The loser dies on the partial unique index");

            // The controller-level recovery: re-read outside the failed transaction
            // and return the winner's order
            Optional<OrderDto> recovered = orderService.findOrderForIdempotentRetry(buyer, IDEMPOTENCY_KEY);
            assertTrue(recovered.isPresent(), "Recovery must find the winner's committed order");
            assertEquals("MH-IDEM-9999", recovered.get().orderNumber(),
                    "Recovery returns the order the winning request created");
        } finally {
            releaseWinner.countDown();
            executor.shutdownNow();
        }
    }
}
