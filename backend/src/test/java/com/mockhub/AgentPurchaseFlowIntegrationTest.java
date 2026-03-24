package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPurchaseFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MandateService mandateService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private MandateRepository mandateRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Full agent purchase flow: mandate -> cart -> checkout -> confirm -> cancel -> spend reversed")
    void fullAgentPurchaseFlow_mandateToCartToCheckoutToConfirmToCancel_spendReversed() {
        // 1. Register a user via REST API helper
        String email = "agent-flow-" + UUID.randomUUID() + "@example.com";
        AuthResponse auth = registerUser(email, "password123", "Agent", "Buyer");
        assertThat(auth).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();

        // 2. Find the User entity by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User not found after registration"));

        // 3. Create test fixtures: venue, section, category, event, ticket, listing
        Venue venue = new Venue();
        venue.setName("Test Arena");
        venue.setSlug("test-arena-" + UUID.randomUUID());
        venue.setAddressLine1("123 Test St");
        venue.setCity("Test City");
        venue.setState("TS");
        venue.setZipCode("12345");
        venue.setCountry("US");
        venue.setCapacity(1000);
        venue.setVenueType("ARENA");
        venue = venueRepository.save(venue);

        Section section = new Section();
        section.setVenue(venue);
        section.setName("Floor");
        section.setSectionType("GENERAL");
        section.setCapacity(100);
        section.setSortOrder(1);
        venue.getSections().add(section);
        venue = venueRepository.save(venue);
        section = venue.getSections().getFirst();

        Category category = new Category();
        category.setName("Test Music " + UUID.randomUUID());
        category.setSlug("test-music-" + UUID.randomUUID());
        category.setSortOrder(99);
        category = categoryRepository.save(category);

        Event event = new Event();
        event.setVenue(venue);
        event.setCategory(category);
        event.setName("Test Concert");
        event.setSlug("test-concert-" + UUID.randomUUID());
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setStatus("ACTIVE");
        event.setBasePrice(new BigDecimal("75.00"));
        event.setTotalTickets(100);
        event.setAvailableTickets(100);
        event.setFeatured(false);
        event = eventRepository.save(event);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setSection(section);
        ticket.setTicketType("STANDARD");
        ticket.setFaceValue(new BigDecimal("75.00"));
        ticket.setStatus("LISTED");
        ticket.setBarcode(UUID.randomUUID().toString().substring(0, 12));
        ticket = ticketRepository.save(ticket);

        Listing listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setSeller(user);
        listing.setListedPrice(new BigDecimal("85.00"));
        listing.setComputedPrice(new BigDecimal("85.00"));
        listing.setPriceMultiplier(new BigDecimal("1.133"));
        listing.setStatus("ACTIVE");
        listing.setListedAt(Instant.now());
        listing = listingRepository.save(listing);

        Long listingId = listing.getId();

        // 4. Create a mandate via MandateService
        CreateMandateRequest mandateRequest = new CreateMandateRequest(
                "test-agent",
                email,
                "PURCHASE",
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                null,  // no category restrictions
                null,  // no event restrictions
                null   // no expiration
        );
        MandateDto mandateDto = mandateService.createMandate(mandateRequest);
        assertThat(mandateDto).isNotNull();
        assertThat(mandateDto.mandateId()).isNotBlank();
        assertThat(mandateDto.status()).isEqualTo("ACTIVE");
        assertThat(mandateDto.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);

        String mandateId = mandateDto.mandateId();

        // 5. Add listing to cart
        cartService.addToCart(user, listingId);

        // 6. Checkout with agent context
        CheckoutRequest checkoutRequest = new CheckoutRequest("mock");
        String idempotencyKey = UUID.randomUUID().toString();
        OrderDto orderDto = orderService.checkout(user, checkoutRequest, idempotencyKey,
                "test-agent", mandateId);
        assertThat(orderDto).isNotNull();
        assertThat(orderDto.orderNumber()).isNotBlank();
        assertThat(orderDto.status()).isEqualTo("PENDING");
        assertThat(orderDto.total()).isGreaterThan(BigDecimal.ZERO);

        String orderNumber = orderDto.orderNumber();
        BigDecimal orderTotal = orderDto.total();

        // 7. Confirm the order
        orderService.confirmOrder(orderNumber);

        // 8. Verify mandate spend was recorded
        Mandate mandateAfterConfirm = mandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new AssertionError("Mandate not found after confirm"));
        assertThat(mandateAfterConfirm.getTotalSpent()).isEqualByComparingTo(orderTotal);

        // 9. Cancel the order
        orderService.cancelOrder(orderNumber);

        // 10. Verify spend was reversed
        Mandate mandateAfterCancel = mandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new AssertionError("Mandate not found after cancel"));
        assertThat(mandateAfterCancel.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
