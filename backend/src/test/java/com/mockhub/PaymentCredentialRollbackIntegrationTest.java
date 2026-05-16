package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.exception.PaymentException;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.paymentcredential.service.PaymentCredentialService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentCredentialRollbackIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MandateService mandateService;

    @Autowired
    private PaymentCredentialService paymentCredentialService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartTools cartTools;

    @Autowired
    private OrderTools orderTools;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PaymentCredentialRepository paymentCredentialRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("MCP confirmation rollback leaves credential active when payment fails")
    void confirmOrder_givenPaymentFailure_rollsBackCredentialConsumption() throws Exception {
        String email = "credential-rollback-" + UUID.randomUUID() + "@example.com";
        AuthResponse auth = registerUser(email, "password123", "Agent", "Buyer");
        assertThat(auth).isNotNull();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User not found after registration"));
        Listing listing = createActiveListing(user);

        MandateDto mandate = mandateService.createMandate(new CreateMandateRequest(
                "test-agent",
                email,
                "PURCHASE",
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                null,
                null,
                null,
                null,
                null));

        JsonNode addToCartJson = objectMapper.readTree(
                cartTools.addToCart(email, listing.getId(), "test-agent", mandate.mandateId()));
        assertThat(addToCartJson.has("error")).isFalse();

        OrderDto pendingOrder = objectMapper.readValue(
                orderTools.checkout(email, "mock", "test-agent", mandate.mandateId()),
                OrderDto.class);
        PaymentCredentialDto credential = paymentCredentialService.issueCredential(
                new CreatePaymentCredentialRequest(
                        email,
                        "test-agent",
                        pendingOrder.total(),
                        "USD",
                        "ONE_TIME",
                        "mock",
                        null));

        when(paymentService.createPaymentIntent(any()))
                .thenReturn(new PaymentIntentDto("pi_failed", "secret", pendingOrder.total(), "USD"));
        when(paymentService.confirmPayment("pi_failed"))
                .thenThrow(new PaymentException("processor down"));

        JsonNode confirmJson = objectMapper.readTree(orderTools.confirmOrder(
                email,
                pendingOrder.orderNumber(),
                "test-agent",
                mandate.mandateId(),
                null,
                credential.credentialId(),
                null));

        assertThat(confirmJson.get("error").asText()).contains("processor down");
        var credentialAfterFailure = paymentCredentialRepository.findByCredentialId(credential.credentialId())
                .orElseThrow(() -> new AssertionError("Payment credential not found after failed confirm"));
        assertThat(credentialAfterFailure.getStatus()).isEqualTo(PaymentCredentialStatus.ACTIVE);
        assertThat(credentialAfterFailure.getConsumedByOrderNumber()).isNull();
        assertThat(orderService.getOrderEntity(pendingOrder.orderNumber()).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    private Listing createActiveListing(User seller) {
        Venue venue = new Venue();
        venue.setName("Rollback Arena");
        venue.setSlug("rollback-arena-" + UUID.randomUUID());
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
        category.setName("Rollback Music " + UUID.randomUUID());
        category.setSlug("rollback-music-" + UUID.randomUUID());
        category.setSortOrder(99);
        category = categoryRepository.save(category);

        Event event = new Event();
        event.setVenue(venue);
        event.setCategory(category);
        event.setName("Rollback Concert");
        event.setSlug("rollback-concert-" + UUID.randomUUID());
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
        listing.setSeller(seller);
        listing.setListedPrice(new BigDecimal("85.00"));
        listing.setComputedPrice(new BigDecimal("85.00"));
        listing.setPriceMultiplier(new BigDecimal("1.133"));
        listing.setStatus("ACTIVE");
        listing.setListedAt(Instant.now());
        return listingRepository.save(listing);
    }
}
