package com.mockhub.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mockhub.AbstractIntegrationTest;
import com.mockhub.admin.service.DemoResetService;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real seeder twice against the real database. The second run is the
 * regression case: it must recognize the already-seeded orders — including
 * navigating their lazy item associations from outside a transaction — and
 * change nothing. (The seeder bean itself is disabled in the test profile via
 * mockhub.demo.seed-accounts=false, so it is constructed manually here.)
 */
class DemoAccountSeederIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ListingRepository listingRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private DemoResetService demoResetService;

    private DemoAccountSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DemoAccountSeeder(userRepository, roleRepository, passwordEncoder,
                listingRepository, orderRepository, cartService, orderService);
        // At least one buyable listing so Bob gets some history
        createListing();
    }

    private void createListing() {
        Venue venue = new Venue();
        venue.setName("Seeder Venue " + System.nanoTime());
        venue.setSlug("seeder-venue-" + System.nanoTime());
        venue.setCity("Boston");
        venue.setState("MA");
        venue.setAddressLine1("1 Seed St");
        venue.setZipCode("02101");
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
        event.setName("Seeder Test Event " + System.nanoTime());
        event.setSlug("seeder-test-" + System.nanoTime());
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
        ticket.setBarcode("SEED-TEST-" + System.nanoTime());
        ticket = ticketRepository.save(ticket);

        Listing listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setListedPrice(new BigDecimal("60.00"));
        listing.setComputedPrice(new BigDecimal("60.00"));
        listing.setPriceMultiplier(BigDecimal.ONE);
        listing.setStatus("ACTIVE");
        listing.setListedAt(Instant.now());
        listing.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        listingRepository.save(listing);
    }

    @Test
    @DisplayName("run twice - seeds alice and bob once, second run is a stable no-op")
    void runTwice_seedsOnce_secondRunIsStableNoOp() {
        seeder.run(null);

        User alice = userRepository.findByEmail("alice@mockhub.com").orElseThrow();
        User bob = userRepository.findByEmail("bob@mockhub.com").orElseThrow();
        assertTrue(passwordEncoder.matches("alicedemo123", alice.getPasswordHash()),
                "Alice keeps the published demo credential");
        assertTrue(bob.isEnabled() && bob.isEmailVerified(), "Bob is ready to log in");

        long seededOrders = countSeededOrders(bob);
        assertTrue(seededOrders >= 1, "Bob should have at least one seeded confirmed order");

        // Second startup: must navigate the existing orders (lazy items included)
        // without error and add nothing
        seeder.run(null);
        assertEquals(seededOrders, countSeededOrders(bob),
                "Second run must not duplicate seeded history");

        // Demo reset preserves the seeded baseline
        demoResetService.resetUser("bob@mockhub.com");
        assertEquals(seededOrders, countSeededOrders(bob),
                "Demo reset must keep the seeded orders confirmed");
    }

    private long countSeededOrders(User bob) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(bob.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 100)).stream()
                .filter(order -> order.getIdempotencyKey() != null
                        && order.getIdempotencyKey().startsWith(DemoAccountSeeder.DEMO_SEED_KEY_PREFIX))
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                .count();
    }
}
