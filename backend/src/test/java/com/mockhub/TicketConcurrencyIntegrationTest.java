package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that optimistic locking prevents double-booking when two users
 * attempt to purchase the same ticket simultaneously.
 *
 * Uses Testcontainers PostgreSQL via AbstractIntegrationTest.
 */
class TicketConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private Listing listing;

    @BeforeEach
    void setUp() {
        // Create role
        Role buyerRole = roleRepository.findByName("ROLE_BUYER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_BUYER")));

        // Create two users
        user1 = createUser("concurrent1@test.com", "User", "One", buyerRole);
        user2 = createUser("concurrent2@test.com", "User", "Two", buyerRole);

        // Create venue, category, section
        Venue venue = new Venue();
        venue.setName("Test Venue " + System.nanoTime());
        venue.setSlug("test-venue-" + System.nanoTime());
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

        // Create event
        Event event = new Event();
        event.setName("Concurrency Test Event " + System.nanoTime());
        event.setSlug("concurrency-test-" + System.nanoTime());
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

        // Create ticket
        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setSection(section);
        ticket.setTicketType("GENERAL_ADMISSION");
        ticket.setFaceValue(new BigDecimal("50.00"));
        ticket.setStatus("AVAILABLE");
        ticket.setBarcode("CONC-TEST-" + System.nanoTime());
        ticket = ticketRepository.save(ticket);

        // Create listing
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

    private User createUser(String email, String firstName, String lastName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRoles(Set.of(role));
            return userRepository.save(user);
        });
    }

    @Test
    @DisplayName("concurrent checkout - two users buying same ticket - exactly one succeeds")
    void concurrentCheckout_sameListing_exactlyOneSucceeds() throws InterruptedException {
        // Both users add the same listing to their carts
        cartService.addToCart(user1, listing.getId());
        cartService.addToCart(user2, listing.getId());

        CheckoutRequest request = new CheckoutRequest("mock");

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Both users attempt checkout simultaneously
        for (User user : new User[]{user1, user2}) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    orderService.checkout(user, request, null);
                    successes.incrementAndGet();
                } catch (ConflictException | OptimisticLockingFailureException
                         | org.springframework.dao.DataIntegrityViolationException ex) {
                    // Any of these indicates the concurrent checkout was properly rejected
                    conflicts.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        done.await();
        executor.shutdown();

        // Exactly one should succeed, one should get a conflict
        assertEquals(1, successes.get(),
                "Exactly one checkout should succeed");
        assertEquals(1, conflicts.get(),
                "Exactly one checkout should fail with conflict");
    }
}
