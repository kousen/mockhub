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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.mcp.tools.AgentApprovalTools;
import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.service.PaymentCredentialService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPurchaseEvidenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MandateService mandateService;

    @Autowired
    private PaymentCredentialService paymentCredentialService;

    @Autowired
    private CartTools cartTools;

    @Autowired
    private OrderTools orderTools;

    @Autowired
    private AgentApprovalTools agentApprovalTools;

    @Autowired
    private com.mockhub.agentapproval.service.AgentPurchaseApprovalService approvalService;

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
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Agent purchase evidence endpoint returns full trail for mandate credential checkout fulfillment flow")
    void getAgentPurchaseEvidence_givenFullAgentFlow_returnsPopulatedEvidence() throws Exception {
        String email = "evidence-flow-" + UUID.randomUUID() + "@example.com";
        AuthResponse auth = registerUser(email, "password123", "Agent", "Evidence");
        assertThat(auth).isNotNull();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User not found after registration"));
        user.setPhone("+15551234567");
        userRepository.save(user);

        Listing listing = createActiveListing(user);
        MandateDto mandate = mandateService.createMandate(new CreateMandateRequest(
                "evidence-agent",
                email,
                "PURCHASE",
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                null,
                null,
                null,
                "APPROVAL_REQUIRED",
                null));

        JsonNode addToCartJson = objectMapper.readTree(
                cartTools.addToCart(email, listing.getId(), "evidence-agent", mandate.mandateId()));
        assertThat(addToCartJson.has("error")).isFalse();

        OrderDto pendingOrder = parseOrder(
                orderTools.checkout(email, "mock", "evidence-agent", mandate.mandateId()));

        AgentPurchaseApprovalDto approval = objectMapper.readValue(agentApprovalTools.proposePurchase(
                email,
                "evidence-agent",
                mandate.mandateId(),
                "{\"listingId\":" + listing.getId() + "}",
                "Best available evidence test ticket",
                pendingOrder.subtotal(),
                pendingOrder.serviceFee(),
                pendingOrder.total(),
                "{\"policyId\":\"default\"}",
                null), AgentPurchaseApprovalDto.class);
        // Approval is web-only now — this is the same service call the
        // /my/approvals page goes through.
        AgentPurchaseApprovalDto approved = approvalService.approve(approval.approvalId(), email);
        assertThat(approved.status()).isEqualTo("APPROVED");

        PaymentCredentialDto credential = paymentCredentialService.issueCredential(
                new CreatePaymentCredentialRequest(
                        email,
                        "evidence-agent",
                        pendingOrder.total(),
                        "USD",
                        "ONE_TIME",
                        "mock",
                        null));

        OrderDto confirmedOrder = parseOrder(orderTools.confirmOrder(
                email,
                pendingOrder.orderNumber(),
                "evidence-agent",
                mandate.mandateId(),
                null,
                credential.credentialId(),
                approval.approvalId()));
        assertThat(confirmedOrder.status()).isEqualTo("CONFIRMED");

        ResponseEntity<AgentPurchaseEvidenceDto> response = restTemplate.exchange(
                "/api/v1/orders/" + confirmedOrder.orderNumber() + "/agent-evidence",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.accessToken())),
                AgentPurchaseEvidenceDto.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        AgentPurchaseEvidenceDto evidence = response.getBody();
        assertThat(evidence).isNotNull();
        assertThat(evidence.mandate().mandateId()).isEqualTo(mandate.mandateId());
        assertThat(evidence.approval().approvalId()).isEqualTo(approval.approvalId());
        assertThat(evidence.approval().status()).isEqualTo("COMPLETED");
        assertThat(evidence.paymentCredential().credentialId()).isEqualTo(credential.credentialId());
        assertThat(evidence.paymentCredential().consumedByOrderNumber()).isEqualTo(confirmedOrder.orderNumber());
        assertThat(evidence.checkout().checkoutId()).isEqualTo(confirmedOrder.orderNumber());
        assertThat(evidence.fulfillment().ticketPdfAvailable()).isTrue();
        assertThat(evidence.fulfillment().emailDispatch().status()).isEqualTo("NOT_PERSISTED");
        assertThat(evidence.fulfillment().smsDispatch().status()).isEqualTo("NOT_PERSISTED");
        assertThat(evidence.fulfillment().ticketArtifacts()).hasSize(1);
        assertThat(evidence.riskSignals())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceRiskSignalDto::signalType)
                .contains("CART_HOLD_ATTEMPT", "CHECKOUT_ATTEMPT");
        assertThat(evidence.evalOutcomes())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceEvalOutcomeDto::ruleName)
                .contains("mandate-authorization", "purchase-approval", "payment-credential", "agent-risk");
        assertThat(evidence.actorTimeline())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceActorStepDto::step)
                .contains("ORDER_CREATED", "ORDER_CONFIRMED", "PURCHASE_APPROVAL_COMPLETED");
    }

    private OrderDto parseOrder(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        assertThat(node.has("error")).isFalse();
        JsonNode orderNode = node.has("order") ? node.get("order") : node;
        return objectMapper.treeToValue(orderNode, OrderDto.class);
    }

    private Listing createActiveListing(User seller) {
        Venue venue = new Venue();
        venue.setName("Evidence Arena");
        venue.setSlug("evidence-arena-" + UUID.randomUUID());
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
        category.setName("Evidence Music " + UUID.randomUUID());
        category.setSlug("evidence-music-" + UUID.randomUUID());
        category.setSortOrder(99);
        category = categoryRepository.save(category);

        Event event = new Event();
        event.setVenue(venue);
        event.setCategory(category);
        event.setName("Evidence Concert");
        event.setSlug("evidence-concert-" + UUID.randomUUID());
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
