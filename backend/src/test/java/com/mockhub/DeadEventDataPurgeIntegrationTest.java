package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.entity.Cart;
import com.mockhub.cart.entity.CartItem;
import com.mockhub.cart.repository.CartRepository;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the dead-event purge bulk deletes against real PostgreSQL:
 * unreferenced inventory and price history for non-ACTIVE events is removed,
 * while anything referenced by order history survives.
 */
@Transactional
class DeadEventDataPurgeIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PriceHistoryRepository priceHistoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Event deadEvent;
    private Event liveEvent;
    private Ticket orphanTicket;
    private Listing orphanListing;
    private Ticket orderedTicket;
    private Listing orderedListing;
    private Listing cartedListing;
    private PriceHistory deadEventSnapshot;
    private PriceHistory freshLiveSnapshot;
    private PriceHistory staleLiveSnapshot;

    @BeforeEach
    void setUp() {
        long stamp = System.nanoTime();

        Venue venue = new Venue();
        venue.setName("Purge Venue " + stamp);
        venue.setSlug("purge-venue-" + stamp);
        venue.setCity("Hartford");
        venue.setState("CT");
        venue.setAddressLine1("1 Purge Way");
        venue.setZipCode("06103");
        venue.setCountry("US");
        venue.setCapacity(100);
        venue.setVenueType("ARENA");

        Section section = new Section();
        section.setVenue(venue);
        section.setName("GA");
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

        deadEvent = createEvent("purge-dead-" + stamp, "CANCELLED", venue, category);
        liveEvent = createEvent("purge-live-" + stamp, "ACTIVE", venue, category);

        orphanTicket = createTicket(deadEvent, section, "PURGE-ORPHAN-" + stamp);
        orphanListing = createListing(deadEvent, orphanTicket, "EXPIRED");

        orderedTicket = createTicket(deadEvent, section, "PURGE-ORDERED-" + stamp);
        orderedListing = createListing(deadEvent, orderedTicket, "SOLD");
        User buyer = createConfirmedOrder(orderedTicket, orderedListing, stamp);

        Ticket cartedTicket = createTicket(deadEvent, section, "PURGE-CARTED-" + stamp);
        cartedListing = createListing(deadEvent, cartedTicket, "ACTIVE");
        createCartWithItem(buyer, cartedListing);

        deadEventSnapshot = createSnapshot(deadEvent, Instant.now().minus(1, ChronoUnit.DAYS));
        freshLiveSnapshot = createSnapshot(liveEvent, Instant.now().minus(1, ChronoUnit.DAYS));
        staleLiveSnapshot = createSnapshot(liveEvent, Instant.now().minus(400, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("purge deletes unreferenced dead-event listings and tickets, keeps order-referenced rows")
    void purge_deletesOrphans_keepsOrderReferencedRows() {
        listingRepository.deleteOrphanedForInactiveEvents();
        ticketRepository.deleteOrphanedForInactiveEvents();
        listingRepository.flush();

        assertFalse(listingRepository.existsById(orphanListing.getId()),
                "Unreferenced dead-event listing should be purged");
        assertFalse(ticketRepository.existsById(orphanTicket.getId()),
                "Unreferenced dead-event ticket should be purged");
        assertTrue(listingRepository.existsById(orderedListing.getId()),
                "Order-referenced listing must survive");
        assertTrue(ticketRepository.existsById(orderedTicket.getId()),
                "Order-referenced ticket must survive");
        assertTrue(listingRepository.existsById(cartedListing.getId()),
                "Cart-referenced listing must survive");
    }

    @Test
    @DisplayName("price history purge removes dead-event and stale rows, keeps fresh live rows")
    void priceHistoryPurge_removesDeadAndStale_keepsFreshLive() {
        priceHistoryRepository.deleteForInactiveEvents();
        priceHistoryRepository.deleteRecordedBefore(Instant.now().minus(90, ChronoUnit.DAYS));
        priceHistoryRepository.flush();

        assertFalse(priceHistoryRepository.existsById(deadEventSnapshot.getId()),
                "Dead-event snapshot should be purged");
        assertFalse(priceHistoryRepository.existsById(staleLiveSnapshot.getId()),
                "Snapshot older than retention should be purged");
        assertTrue(priceHistoryRepository.existsById(freshLiveSnapshot.getId()),
                "Fresh live-event snapshot must survive");
    }

    private Event createEvent(String slug, String status, Venue venue, Category category) {
        Event event = new Event();
        event.setName(slug);
        event.setSlug(slug);
        event.setDescription("Purge test event");
        event.setVenue(venue);
        event.setCategory(category);
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setStatus(status);
        event.setBasePrice(new BigDecimal("50.00"));
        event.setMinPrice(new BigDecimal("40.00"));
        event.setMaxPrice(new BigDecimal("100.00"));
        event.setTotalTickets(100);
        event.setAvailableTickets(100);
        return eventRepository.save(event);
    }

    private Ticket createTicket(Event event, Section section, String barcode) {
        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setSection(section);
        ticket.setTicketType("GENERAL_ADMISSION");
        ticket.setFaceValue(new BigDecimal("50.00"));
        ticket.setStatus("AVAILABLE");
        ticket.setBarcode(barcode);
        return ticketRepository.save(ticket);
    }

    private Listing createListing(Event event, Ticket ticket, String status) {
        Listing listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setListedPrice(new BigDecimal("75.00"));
        listing.setComputedPrice(new BigDecimal("75.00"));
        listing.setPriceMultiplier(BigDecimal.ONE);
        listing.setStatus(status);
        listing.setListedAt(Instant.now());
        return listingRepository.save(listing);
    }

    private User createConfirmedOrder(Ticket ticket, Listing listing, long stamp) {
        Role role = roleRepository.findByName("ROLE_BUYER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_BUYER")));
        User user = new User();
        user.setEmail("purge-" + stamp + "@test.com");
        user.setFirstName("Purge");
        user.setLastName("Tester");
        user.setPasswordHash(passwordEncoder.encode("test-password"));
        user.setRoles(java.util.Set.of(role));
        user = userRepository.save(user);

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber("MH-PURGE-" + (stamp % 100000000L));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotal(new BigDecimal("75.00"));
        order.setServiceFee(new BigDecimal("7.50"));
        order.setTotal(new BigDecimal("82.50"));
        order.setPaymentMethod("mock");
        order.setConfirmedAt(Instant.now());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setListing(listing);
        item.setTicket(ticket);
        item.setPricePaid(new BigDecimal("75.00"));
        order.getItems().add(item);
        orderRepository.save(order);
        return user;
    }

    private void createCartWithItem(User user, Listing listing) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setListing(listing);
        item.setPriceAtAdd(listing.getComputedPrice());
        item.setAddedAt(Instant.now());
        cart.getItems().add(item);
        cartRepository.save(cart);
    }

    private PriceHistory createSnapshot(Event event, Instant recordedAt) {
        PriceHistory snapshot = new PriceHistory();
        snapshot.setEvent(event);
        snapshot.setPrice(new BigDecimal("42.00"));
        snapshot.setMultiplier(new BigDecimal("1.000"));
        snapshot.setSupplyRatio(new BigDecimal("1.0000"));
        snapshot.setDaysToEvent(30);
        snapshot.setRecordedAt(recordedAt);
        return priceHistoryRepository.save(snapshot);
    }
}
