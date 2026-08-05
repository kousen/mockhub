package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpLineItem;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.commerce.dto.CommercePolicyDto;
import com.mockhub.commerce.service.CommercePolicyService;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderItemDto;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;
import com.mockhub.paymentcredential.service.PaymentCredentialService;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcpCheckoutServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private EvalRunner evalRunner;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CommercePolicyService commercePolicyService;

    @Mock
    private AgentPurchaseApprovalService approvalService;

    @Mock
    private MandateService mandateService;

    @Mock
    private PaymentCredentialService paymentCredentialService;

    @Mock
    private AgentRiskService agentRiskService;

    @InjectMocks
    private AcpCheckoutService acpCheckoutService;

    private static final String AGENT_ID = "shopping-agent";
    private static final String MANDATE_ID = "mandate-123";

    private User testUser;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@test.com");

        List<OrderItemDto> orderItems = List.of(
                new OrderItemDto(1L, 10L, 100L, "Test Concert", "test-concert",
                        "Floor", "A", "1", "GENERAL", new BigDecimal("50.00"))
        );

        testOrderDto = new OrderDto(
                1L,
                "MH-20260323-0001",
                "PENDING",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                null,
                Instant.now(),
                orderItems,
                null,
                null
        );

        lenient().when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(EvalResult.pass("ok"))));
        lenient().when(listingRepository.findById(any()))
                .thenAnswer(invocation -> Optional.of(createListing(invocation.getArgument(0))));
        lenient().when(cartService.getCartDto(testUser))
                .thenReturn(new CartDto(1L, 1L, List.of(), new BigDecimal("55.00"), 1, null));
        lenient().when(commercePolicyService.getDefaultPolicy()).thenReturn(createCommercePolicy());
        lenient().when(commercePolicyService.getPolicyForEvent("test-concert"))
                .thenReturn(createCommercePolicy("test-concert"));
    }

    private CommercePolicyDto createCommercePolicy() {
        return new CommercePolicyDto(
                "policy", "v1", "all_mockhub_ticket_purchases", null,
                List.of(), "support@mockhub.dev", "/support",
                "/api/v1/commerce/policies/default", Instant.now());
    }

    private CommercePolicyDto createCommercePolicy(String eventSlug) {
        return new CommercePolicyDto(
                "policy", "v1", "event:" + eventSlug, eventSlug,
                List.of(), "support@mockhub.dev", "/support",
                "/api/v1/commerce/policies/events/" + eventSlug, Instant.now());
    }

    private AcpCheckoutRequest createCheckoutRequest(String buyerEmail, List<AcpLineItem> lineItems,
                                                     String paymentMethod, String idempotencyKey) {
        return new AcpCheckoutRequest(buyerEmail, lineItems, AGENT_ID, MANDATE_ID, paymentMethod, idempotencyKey);
    }

    private AcpUpdateRequest createUpdateRequest(List<AcpLineItem> addItems, List<Long> removeListingIds) {
        return new AcpUpdateRequest(AGENT_ID, MANDATE_ID, addItems, removeListingIds);
    }

    private CartDto createCartWithFloorTicket() {
        CartItemDto item = new CartItemDto(
                1L,
                10L,
                "Test Concert",
                "test-concert",
                "Floor",
                "A",
                "1",
                "GENERAL",
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                null);
        return new CartDto(1L, 1L, List.of(item), new BigDecimal("55.00"), 1, null);
    }

    private Order createAgentOrder(String orderNumber, String paymentMethod) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(testUser);
        order.setAgentId(AGENT_ID);
        order.setMandateId(MANDATE_ID);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING);
        order.setTotal(new BigDecimal("55.00"));
        return order;
    }

    private Listing createListing(long listingId) {
        com.mockhub.event.entity.Event event = new com.mockhub.event.entity.Event();
        event.setSlug("test-concert");
        event.setName("Test Concert");
        com.mockhub.event.entity.Category category = new com.mockhub.event.entity.Category();
        category.setSlug("concerts");
        event.setCategory(category);
        event.setStatus("ACTIVE");
        event.setEventDate(Instant.now().plusSeconds(86_400));

        Listing listing = new Listing();
        listing.setId(listingId);
        listing.setEvent(event);
        listing.setStatus("ACTIVE");
        listing.setComputedPrice(new BigDecimal("50.00"));

        com.mockhub.ticket.entity.Ticket ticket = new com.mockhub.ticket.entity.Ticket();
        com.mockhub.venue.entity.Section section = new com.mockhub.venue.entity.Section();
        section.setName("Floor");
        ticket.setSection(section);
        listing.setTicket(ticket);
        return listing;
    }

    @Test
    @DisplayName("createCheckout - valid request - returns CREATED response")
    void createCheckout_validRequest_returnsCreatedResponse() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "mock",
                "idem-123"
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(testUser, 10L)).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 1, null));
        when(orderService.checkout(
                eq(testUser), any(CheckoutRequest.class), eq("idem-123"), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);

        assertNotNull(response);
        assertEquals("MH-20260323-0001", response.checkoutId());
        assertEquals("CREATED", response.status());
        assertEquals("buyer@test.com", response.buyerEmail());
        assertEquals(1, response.lineItems().size());
        assertEquals("USD", response.pricing().currency());
        assertEquals(new BigDecimal("55.00"), response.pricing().total());
        assertNotNull(response.commercePolicy());
        assertEquals("test-concert", response.commercePolicy().eventSlug());

        verify(cartService).clearCart(testUser);
        verify(cartService).addToCart(testUser, 10L);
        verify(orderService).checkout(eq(testUser), any(CheckoutRequest.class), eq("idem-123"), eq(AGENT_ID), eq(MANDATE_ID));
    }

    @Test
    @DisplayName("createCheckout - uppercase payment method - normalizes before checkout")
    void createCheckout_uppercasePaymentMethod_normalizesBeforeCheckout() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "STRIPE",
                "idem-123"
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(testUser, 10L)).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 1, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq("idem-123"), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(testOrderDto);

        acpCheckoutService.createCheckout(request);

        ArgumentCaptor<CheckoutRequest> requestCaptor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(orderService).checkout(
                eq(testUser), requestCaptor.capture(), eq("idem-123"), eq(AGENT_ID), eq(MANDATE_ID));
        assertEquals("stripe", requestCaptor.getValue().paymentMethod());
    }

    @Test
    @DisplayName("createCheckout - verbose payment method - rejects before cart mutation")
    void createCheckout_verbosePaymentMethod_rejectsBeforeCartMutation() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "use the mock payment fallback for this purchase",
                "idem-123"
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                acpCheckoutService.createCheckout(request));

        assertEquals("Unsupported payment method 'use the mock payment fallback for this purchase'. "
                + "Supported payment methods are: mock, stripe", exception.getMessage());
        verify(cartService, never()).clearCart(any());
        verify(orderService, never()).checkout(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createCheckout - cart item authorized by mandate - returns CREATED response")
    void createCheckout_cartItemAuthorizedByMandate_returnsCreatedResponse() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "mock",
                "idem-123"
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(testUser, 10L)).thenReturn(createCartWithFloorTicket());
        when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
        // The cart subtotal is 55.00; the buyer is charged 60.50 once the service fee is
        // applied, and that is the amount the mandate must authorize.
        when(mandateService.validateAction(AGENT_ID, "buyer@test.com", "PURCHASE",
                new BigDecimal("60.50"), null, "test-concert", MANDATE_ID, "Floor"))
                .thenReturn(true);
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq("idem-123"), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);

        assertEquals("CREATED", response.status());
        verify(mandateService).validateAction(AGENT_ID, "buyer@test.com", "PURCHASE",
                new BigDecimal("60.50"), null, "test-concert", MANDATE_ID, "Floor");
    }

    @Test
    @DisplayName("createCheckout - cart item not authorized by mandate - throws ConflictException")
    void createCheckout_cartItemNotAuthorizedByMandate_throwsConflictException() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1)),
                "mock",
                "idem-123"
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(testUser, 10L)).thenReturn(createCartWithFloorTicket());
        when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
        when(mandateService.validateAction(AGENT_ID, "buyer@test.com", "PURCHASE",
                new BigDecimal("60.50"), null, "test-concert", MANDATE_ID, "Floor"))
                .thenReturn(false);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                acpCheckoutService.createCheckout(request));

        assertEquals("ACP cart validation failed: Mandate does not authorize cart item listing 10",
                exception.getMessage());
        verify(orderService, never()).checkout(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createCheckout - unknown buyer email - throws ResourceNotFoundException")
    void createCheckout_unknownBuyerEmail_throwsResourceNotFoundException() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "nobody@test.com",
                List.of(new AcpLineItem(10L, 1)),
                null,
                null
        );

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                acpCheckoutService.createCheckout(request));
    }

    @Test
    @DisplayName("createCheckout - multiple line items - adds all to cart")
    void createCheckout_multipleLineItems_addsAllToCart() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "buyer@test.com",
                List.of(new AcpLineItem(10L, 1), new AcpLineItem(20L, 1)),
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 2, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);

        assertNotNull(response);
        verify(cartService).addToCart(testUser, 10L);
        verify(cartService).addToCart(testUser, 20L);
    }

    @Test
    @DisplayName("completeCheckout - pending order - returns COMPLETED response")
    void completeCheckout_pendingOrder_returnsCompletedResponse() {
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items(),
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001"))
                .thenReturn(testOrderDto)
                .thenReturn(confirmedOrder);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));
        when(paymentService.createPaymentIntent(any(Order.class)))
                .thenReturn(new PaymentIntentDto("pi_test", "secret", new BigDecimal("55.00"), "USD"));

        AcpCheckoutResponse response = acpCheckoutService.completeCheckout(
                "MH-20260323-0001", "buyer@test.com",
                new com.mockhub.acp.dto.AcpCompleteRequest(AGENT_ID, MANDATE_ID, null, null, null));

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertNotNull(response.completedAt());
        assertEquals("test-concert", response.commercePolicy().eventSlug());

        verify(paymentService).confirmPayment("pi_test");
    }

    @Test
    @DisplayName("completeCheckout - payment credential ID - validates and consumes before payment")
    void completeCheckout_paymentCredentialId_validatesAndConsumesBeforePayment() {
        Order order = createAgentOrder("MH-20260323-0001", "mock");
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items(),
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001"))
                .thenReturn(testOrderDto)
                .thenReturn(confirmedOrder);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        when(paymentService.createPaymentIntent(any(Order.class)))
                .thenReturn(new PaymentIntentDto("pi_test", "secret", new BigDecimal("55.00"), "USD"));

        AcpCheckoutResponse response = acpCheckoutService.completeCheckout(
                "MH-20260323-0001", "buyer@test.com",
                new com.mockhub.acp.dto.AcpCompleteRequest(
                        AGENT_ID, MANDATE_ID, null, null, "cred-123"));

        assertEquals("COMPLETED", response.status());
        verify(paymentCredentialService).authorizeForPayment(
                "cred-123", "buyer@test.com", AGENT_ID,
                new BigDecimal("55.00"), "USD", "mock", "MH-20260323-0001");
        verify(paymentCredentialService).consumeForPayment("cred-123", "MH-20260323-0001");
        verify(paymentService).confirmPayment("pi_test");
    }

    @Test
    @DisplayName("completeCheckout - invalid payment credential - does not confirm payment")
    void completeCheckout_invalidPaymentCredential_doesNotConfirmPayment() {
        Order order = createAgentOrder("MH-20260323-0001", "mock");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        doThrow(new ConflictException("Order total exceeds payment credential limit"))
                .when(paymentCredentialService).authorizeForPayment(
                        "cred-123", "buyer@test.com", AGENT_ID,
                        new BigDecimal("55.00"), "USD", "mock", "MH-20260323-0001");

        ConflictException exception = assertThrows(ConflictException.class,
                () -> acpCheckoutService.completeCheckout(
                        "MH-20260323-0001", "buyer@test.com",
                        new com.mockhub.acp.dto.AcpCompleteRequest(
                                AGENT_ID, MANDATE_ID, null, null, "cred-123")));

        assertEquals("Order total exceeds payment credential limit", exception.getMessage());
        verify(paymentCredentialService, never()).consumeForPayment(any(), any());
        verify(paymentService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("completeCheckout - approved approval ID - validates and marks completed")
    void completeCheckout_approvedApprovalId_validatesAndMarksCompleted() {
        Order order = createAgentOrder("MH-20260323-0001", "mock");
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items(),
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001"))
                .thenReturn(testOrderDto)
                .thenReturn(confirmedOrder);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        when(paymentService.createPaymentIntent(any(Order.class)))
                .thenReturn(new PaymentIntentDto("pi_test", "secret", new BigDecimal("55.00"), "USD"));

        acpCheckoutService.completeCheckout(
                "MH-20260323-0001",
                "buyer@test.com",
                new com.mockhub.acp.dto.AcpCompleteRequest(
                        AGENT_ID, MANDATE_ID, null, "approval-123", null));

        verify(approvalService).validateApprovedForCompletion(
                "approval-123", "buyer@test.com", AGENT_ID, MANDATE_ID, order);
        verify(approvalService).markCompleted("approval-123", "MH-20260323-0001");
    }

    @Test
    @DisplayName("completeCheckout - approval required mandate without approval ID - throws ConflictException")
    void completeCheckout_approvalRequiredMandateWithoutApprovalId_throwsConflictException() {
        Order order = createAgentOrder("MH-20260323-0001", "mock");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        when(mandateService.approvalRequired(AGENT_ID, "buyer@test.com", MANDATE_ID)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> acpCheckoutService.completeCheckout(
                        "MH-20260323-0001",
                        "buyer@test.com",
                        new com.mockhub.acp.dto.AcpCompleteRequest(AGENT_ID, MANDATE_ID, null, null, null)));

        assertEquals("Mandate requires an approved purchase approval before completion", exception.getMessage());
        verify(paymentService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("completeCheckout - mismatched payment intent - throws ConflictException")
    void completeCheckout_mismatchedPaymentIntent_throwsConflictException() {
        Order order = createAgentOrder("MH-20260323-0001", "stripe");
        order.setPaymentIntentId("pi_original");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);

        assertThrows(ConflictException.class, () ->
                acpCheckoutService.completeCheckout(
                        "MH-20260323-0001", "buyer@test.com",
                        new com.mockhub.acp.dto.AcpCompleteRequest(
                                AGENT_ID, MANDATE_ID, "pi_different", null, null)));

        verify(paymentService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("cancelCheckout - pending order - returns CANCELLED response")
    void cancelCheckout_pendingOrder_returnsCancelledResponse() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));

        AcpCheckoutResponse response = acpCheckoutService.cancelCheckout(
                "MH-20260323-0001", "buyer@test.com",
                new com.mockhub.acp.dto.AcpActionRequest(AGENT_ID, MANDATE_ID));

        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        assertNull(response.completedAt());
        assertNotNull(response.commercePolicy());
        assertEquals("test-concert", response.commercePolicy().eventSlug());

        verify(orderService).failOrder("MH-20260323-0001");
    }

    @Test
    @DisplayName("updateCheckout - non-pending order - throws ConflictException")
    void updateCheckout_nonPendingOrder_throwsConflictException() {
        OrderDto confirmedOrder = new OrderDto(
                1L,
                "MH-20260323-0001",
                "CONFIRMED",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "mock",
                Instant.now(),
                Instant.now(),
                testOrderDto.items(),
                null,
                null
        );

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(confirmedOrder);

        AcpUpdateRequest updateRequest = createUpdateRequest(List.of(new AcpLineItem(30L, 1)), null);

        assertThrows(ConflictException.class, () ->
                acpCheckoutService.updateCheckout("MH-20260323-0001", updateRequest, "buyer@test.com"));
    }

    @Test
    @DisplayName("getCheckout - valid checkout - returns response")
    void getCheckout_validCheckout_returnsResponse() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);

        AcpCheckoutResponse response = acpCheckoutService.getCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertNotNull(response);
        assertEquals("MH-20260323-0001", response.checkoutId());
        assertEquals("CREATED", response.status());
        assertEquals("buyer@test.com", response.buyerEmail());
    }

    @Test
    @DisplayName("updateCheckout - pending order with add and remove - preserves kept items and adds new")
    void updateCheckout_pendingOrderWithAddAndRemove_preservesKeptItemsAndAddsNew() {
        List<OrderItemDto> existingItems = List.of(
                new OrderItemDto(1L, 10L, 100L, "Concert A", "concert-a",
                        "Floor", "A", "1", "GENERAL", new BigDecimal("50.00")),
                new OrderItemDto(2L, 20L, 200L, "Concert B", "concert-b",
                        "Balcony", "B", "2", "GENERAL", new BigDecimal("60.00"))
        );
        OrderDto pendingOrder = new OrderDto(
                1L, "MH-20260323-0001", "PENDING",
                new BigDecimal("110.00"), new BigDecimal("11.00"), new BigDecimal("121.00"),
                "mock", null, Instant.now(), existingItems, null, null);

        OrderDto newOrder = new OrderDto(
                2L, "MH-20260323-0002", "PENDING",
                new BigDecimal("80.00"), new BigDecimal("8.00"), new BigDecimal("88.00"),
                "mock", null, Instant.now(), List.of(
                new OrderItemDto(1L, 10L, 100L, "Concert A", "concert-a",
                        "Floor", "A", "1", "GENERAL", new BigDecimal("50.00")),
                new OrderItemDto(3L, 30L, 300L, "Concert C", "concert-c",
                        "VIP", "C", "3", "GENERAL", new BigDecimal("30.00"))
        ), null, null);

        AcpUpdateRequest updateRequest = createUpdateRequest(List.of(new AcpLineItem(30L, 1)), List.of(20L));

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(pendingOrder);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 2, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(newOrder);

        AcpCheckoutResponse response = acpCheckoutService.updateCheckout(
                "MH-20260323-0001", updateRequest, "buyer@test.com");

        assertNotNull(response);
        assertEquals("MH-20260323-0002", response.checkoutId());
        verify(orderService).failOrder("MH-20260323-0001");
        verify(cartService).clearCart(testUser);
        // Kept listing 10L (not in removeListingIds), removed listing 20L
        verify(cartService).addToCart(testUser, 10L);
        // Added new listing 30L
        verify(cartService).addToCart(testUser, 30L);
    }

    @Test
    @DisplayName("updateCheckout - stripe order - preserves original payment method")
    void updateCheckout_givenStripeOrder_preservesOriginalPaymentMethod() {
        List<OrderItemDto> existingItems = List.of(
                new OrderItemDto(1L, 10L, 100L, "Concert A", "concert-a",
                        "Floor", "A", "1", "GENERAL", new BigDecimal("50.00")));
        OrderDto pendingOrder = new OrderDto(
                1L, "MH-20260323-0001", "PENDING",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "stripe", null, Instant.now(), existingItems, null, null);
        OrderDto newOrder = new OrderDto(
                2L, "MH-20260323-0002", "PENDING",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "stripe", null, Instant.now(), existingItems, null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(pendingOrder);
        when(orderService.getOrderEntity("MH-20260323-0001"))
                .thenReturn(createAgentOrder("MH-20260323-0001", "stripe"));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 1, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(newOrder);

        acpCheckoutService.updateCheckout(
                "MH-20260323-0001", createUpdateRequest(null, null), "buyer@test.com");

        ArgumentCaptor<CheckoutRequest> requestCaptor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(orderService).checkout(
                eq(testUser), requestCaptor.capture(), eq(null), eq(AGENT_ID), eq(MANDATE_ID));
        assertEquals("stripe", requestCaptor.getValue().paymentMethod());
    }

    @Test
    @DisplayName("updateCheckout - pending order with null removeListingIds - keeps all existing items")
    void updateCheckout_pendingOrderWithNullRemoveListingIds_keepsAllExistingItems() {
        AcpUpdateRequest updateRequest = createUpdateRequest(List.of(new AcpLineItem(30L, 1)), null);

        OrderDto newOrder = new OrderDto(
                2L, "MH-20260323-0002", "PENDING",
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("110.00"),
                "mock", null, Instant.now(), testOrderDto.items(), null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 2, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(newOrder);

        AcpCheckoutResponse response = acpCheckoutService.updateCheckout(
                "MH-20260323-0001", updateRequest, "buyer@test.com");

        assertNotNull(response);
        verify(orderService).failOrder("MH-20260323-0001");
        // Original item 10L is kept
        verify(cartService).addToCart(testUser, 10L);
        // New item 30L is added
        verify(cartService).addToCart(testUser, 30L);
    }

    @Test
    @DisplayName("updateCheckout - pending order with null addItems - only keeps existing items minus removals")
    void updateCheckout_pendingOrderWithNullAddItems_onlyKeepsExistingItems() {
        AcpUpdateRequest updateRequest = createUpdateRequest(null, null);

        OrderDto newOrder = new OrderDto(
                2L, "MH-20260323-0002", "PENDING",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "mock", null, Instant.now(), testOrderDto.items(), null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));
        when(cartService.addToCart(eq(testUser), any(Long.class))).thenReturn(
                new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 1, null));
        when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), eq(null), eq(AGENT_ID), eq(MANDATE_ID)))
                .thenReturn(newOrder);

        AcpCheckoutResponse response = acpCheckoutService.updateCheckout(
                "MH-20260323-0001", updateRequest, "buyer@test.com");

        assertNotNull(response);
        verify(cartService).addToCart(testUser, 10L);
    }

    @Test
    @DisplayName("resolveUser - given null email - throws IllegalArgumentException")
    void resolveUser_givenNullEmail_throwsIllegalArgumentException() {
        AcpCheckoutRequest request = createCheckoutRequest(
                null,
                List.of(new AcpLineItem(10L, 1)),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () ->
                acpCheckoutService.createCheckout(request));
    }

    @Test
    @DisplayName("resolveUser - given blank email - throws IllegalArgumentException")
    void resolveUser_givenBlankEmail_throwsIllegalArgumentException() {
        AcpCheckoutRequest request = createCheckoutRequest(
                "   ",
                List.of(new AcpLineItem(10L, 1)),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () ->
                acpCheckoutService.createCheckout(request));
    }

    @Test
    @DisplayName("cancelCheckout - returns correct line item details")
    void cancelCheckout_returnsCorrectLineItemDetails() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));

        AcpCheckoutResponse response = acpCheckoutService.cancelCheckout(
                "MH-20260323-0001", "buyer@test.com",
                new com.mockhub.acp.dto.AcpActionRequest(AGENT_ID, MANDATE_ID));

        assertEquals(1, response.lineItems().size());
        assertEquals(10L, response.lineItems().getFirst().listingId());
        assertEquals("Test Concert", response.lineItems().getFirst().eventName());
        assertEquals("test-concert", response.lineItems().getFirst().eventSlug());
        assertEquals("Floor", response.lineItems().getFirst().section());
        assertEquals("A", response.lineItems().getFirst().row());
        assertEquals("1", response.lineItems().getFirst().seat());
        assertEquals(new BigDecimal("50.00"), response.lineItems().getFirst().unitPrice());
        assertEquals(new BigDecimal("55.00"), response.pricing().total());
        assertEquals("USD", response.pricing().currency());
    }

    @Test
    @DisplayName("completeCheckout - maps CONFIRMED status to COMPLETED")
    void completeCheckout_mapsConfirmedStatusToCompleted() {
        OrderDto confirmedOrder = new OrderDto(
                1L, "MH-20260323-0001", "CONFIRMED",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "mock", Instant.now(), Instant.now(), testOrderDto.items(), null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001"))
                .thenReturn(testOrderDto)
                .thenReturn(confirmedOrder);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(createAgentOrder("MH-20260323-0001", "mock"));
        when(paymentService.createPaymentIntent(any(Order.class)))
                .thenReturn(new PaymentIntentDto("pi_test", "secret", new BigDecimal("55.00"), "USD"));

        AcpCheckoutResponse response = acpCheckoutService.completeCheckout(
                "MH-20260323-0001", "buyer@test.com",
                new com.mockhub.acp.dto.AcpCompleteRequest(AGENT_ID, MANDATE_ID, null, null, null));

        assertEquals("COMPLETED", response.status());
    }

    @Test
    @DisplayName("getCheckout - maps FAILED status to CANCELLED")
    void getCheckout_mapsFailedStatusToCancelled() {
        OrderDto failedOrder = new OrderDto(
                1L, "MH-20260323-0001", "FAILED",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "mock", null, Instant.now(), testOrderDto.items(), null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(failedOrder);

        AcpCheckoutResponse response = acpCheckoutService.getCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertEquals("CANCELLED", response.status());
    }

    @Test
    @DisplayName("updateCheckout - given critical eval failure during revalidation - throws ConflictException")
    void updateCheckout_givenCriticalEvalFailure_throwsConflictException() {
        Order order = createAgentOrder("MH-20260323-0001", "mock");
        com.mockhub.order.entity.OrderItem orderItem = new com.mockhub.order.entity.OrderItem();
        orderItem.setOrder(order);
        Listing listing = createListing(10L);
        orderItem.setListing(listing);
        com.mockhub.ticket.entity.Ticket ticket = new com.mockhub.ticket.entity.Ticket();
        ticket.setSection(listing.getTicket().getSection());
        orderItem.setTicket(ticket);
        orderItem.setPricePaid(new BigDecimal("50.00"));
        order.setItems(List.of(orderItem));
        order.setTotal(new BigDecimal("55.00"));

        AcpUpdateRequest updateRequest = createUpdateRequest(null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(
                EvalResult.fail("mandate", com.mockhub.eval.dto.EvalSeverity.CRITICAL, "Mandate expired"))));

        assertThrows(ConflictException.class, () ->
                acpCheckoutService.updateCheckout("MH-20260323-0001", updateRequest, "buyer@test.com"));

        verify(orderService, never()).failOrder(any());
    }

    @Test
    @DisplayName("completeCheckout - mandate no longer authorizes the order - refuses before charging")
    void completeCheckout_mandateNoLongerAuthorizes_refusesBeforeCharging() {
        // Completion is the step that moves money, so it must re-authorize against the order
        // total. Previously only updateCheckout did, and a mandate could be revoked or
        // outgrown between creating the checkout and completing it.
        Order order = createAgentOrder("MH-20260323-0001", "mock");
        Listing listing = createListing(10L);
        com.mockhub.order.entity.OrderItem orderItem = new com.mockhub.order.entity.OrderItem();
        orderItem.setListing(listing);
        order.setItems(List.of(orderItem));

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(testOrderDto);
        when(orderService.getOrderEntity("MH-20260323-0001")).thenReturn(order);
        when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(
                EvalResult.fail("mandate-authorization",
                        com.mockhub.eval.dto.EvalSeverity.CRITICAL,
                        "Mandate does not authorize this action"))));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                acpCheckoutService.completeCheckout("MH-20260323-0001", "buyer@test.com",
                        new com.mockhub.acp.dto.AcpCompleteRequest(AGENT_ID, MANDATE_ID, null, null, null)));

        assertEquals("ACP confirmation validation failed: mandate-authorization: "
                + "Mandate does not authorize this action", exception.getMessage());
        verify(paymentService, never()).createPaymentIntent(any(Order.class));
    }

    @Test
    @DisplayName("getCheckout - maps unknown status directly")
    void getCheckout_mapsUnknownStatusDirectly() {
        OrderDto unknownOrder = new OrderDto(
                1L, "MH-20260323-0001", "PROCESSING",
                new BigDecimal("50.00"), new BigDecimal("5.00"), new BigDecimal("55.00"),
                "mock", null, Instant.now(), testOrderDto.items(), null, null);

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrder(testUser, "MH-20260323-0001")).thenReturn(unknownOrder);

        AcpCheckoutResponse response = acpCheckoutService.getCheckout(
                "MH-20260323-0001", "buyer@test.com");

        assertEquals("PROCESSING", response.status());
    }
}
