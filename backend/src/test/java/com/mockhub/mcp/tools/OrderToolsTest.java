package com.mockhub.mcp.tools;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.ai.service.ChatContext;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.service.CalendarService;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;
import com.mockhub.paymentcredential.service.PaymentCredentialService;
import com.mockhub.mandate.service.MandateService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.event.entity.Event;
import com.mockhub.event.entity.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderToolsTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private EvalRunner evalRunner;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CalendarService calendarService;

    @Mock
    private AgentPurchaseApprovalService approvalService;

    @Mock
    private MandateService mandateService;

    @Mock
    private PaymentCredentialService paymentCredentialService;

    @Mock
    private AgentRiskService agentRiskService;

    private static final String AGENT_ID = "shopping-agent";
    private static final String MANDATE_ID = "mandate-123";

    private ObjectMapper objectMapper;
    private OrderTools orderTools;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderTools = new OrderTools(orderService, calendarService, userRepository, cartService,
                evalRunner, paymentService, approvalService, mandateService, paymentCredentialService,
                agentRiskService, objectMapper);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
    }

    private void stubUserLookup(String email) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    }

    private void stubPassingEval() {
        when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(EvalResult.pass("mandate"))));
    }

    private CartDto createCartWithFloorTicket() {
        CartItemDto item = new CartItemDto(
                1L,
                42L,
                "Test Event",
                "test-event",
                "Floor",
                "A",
                "1",
                "GENERAL",
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.TEN,
                null);
        return new CartDto(null, 1L, List.of(item), java.math.BigDecimal.TEN, 1, null);
    }

    private Order createAgentOrderEntity(String orderNumber) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setAgentId(AGENT_ID);
        order.setMandateId(MANDATE_ID);
        order.setPaymentMethod("mock");
        order.setTotal(java.math.BigDecimal.TEN);

        Event event = new Event();
        event.setName("Test Event");
        event.setSlug("test-event");
        event.setStatus("ACTIVE");
        event.setEventDate(java.time.Instant.now().plusSeconds(3600));
        Category category = new Category();
        category.setSlug("concerts");
        event.setCategory(category);

        Listing listing = new Listing();
        listing.setEvent(event);
        listing.setStatus("ACTIVE");
        listing.setComputedPrice(java.math.BigDecimal.TEN);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        listing.setTicket(ticket);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setListing(listing);
        orderItem.setTicket(ticket);
        orderItem.setPricePaid(java.math.BigDecimal.TEN);
        order.setItems(List.of(orderItem));

        return order;
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("given valid email and payment method - returns order JSON")
        void givenValidEmailAndPaymentMethod_returnsOrderJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
            stubPassingEval();
            when(mandateService.validateAction(AGENT_ID, "buyer@example.com", "PURCHASE",
                    java.math.BigDecimal.TEN, null, "test-event", MANDATE_ID, "Floor"))
                    .thenReturn(true);
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(
                    eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenReturn(orderDto);

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            verify(orderService).checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID));
            verify(mandateService).validateAction(AGENT_ID, "buyer@example.com", "PURCHASE",
                    java.math.BigDecimal.TEN, null, "test-event", MANDATE_ID, "Floor");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given uppercase payment method - normalizes before checkout")
        void givenUppercasePaymentMethod_normalizesBeforeCheckout() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
            stubPassingEval();
            when(mandateService.validateAction(AGENT_ID, "buyer@example.com", "PURCHASE",
                    java.math.BigDecimal.TEN, null, "test-event", MANDATE_ID, "Floor"))
                    .thenReturn(true);
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenReturn(orderDto);

            orderTools.checkout("buyer@example.com", "STRIPE", AGENT_ID, MANDATE_ID);

            ArgumentCaptor<CheckoutRequest> requestCaptor = ArgumentCaptor.forClass(CheckoutRequest.class);
            verify(orderService).checkout(
                    eq(testUser), requestCaptor.capture(), any(), eq(AGENT_ID), eq(MANDATE_ID));
            assertEquals("stripe", requestCaptor.getValue().paymentMethod());
        }

        @Test
        @DisplayName("given verbose payment method - returns error before checkout")
        void givenVerbosePaymentMethod_returnsErrorBeforeCheckout() {
            String result = orderTools.checkout(
                    "buyer@example.com", "use the mock payment fallback for this purchase", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Unsupported payment method"), "Result should explain supported methods");
            verify(orderService, never()).checkout(any(), any(), any(), any(), any());
            verify(cartService, never()).getCartDto(any());
        }

        @Test
        @DisplayName("given risk warning - returns order with warnings")
        void givenRiskWarning_returnsOrderWithWarnings() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
            stubPassingEval();
            when(agentRiskService.recordCheckoutAttempt(
                    "buyer@example.com", AGENT_ID, null, java.math.BigDecimal.TEN, "CHECKOUT"))
                    .thenReturn(List.of("agent-risk: high spend attempt"));
            when(mandateService.validateAction(AGENT_ID, "buyer@example.com", "PURCHASE",
                    java.math.BigDecimal.TEN, null, "test-event", MANDATE_ID, "Floor"))
                    .thenReturn(true);
            OrderDto orderDto = new OrderDto(
                    null, "MH-1", "PENDING", null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenReturn(orderDto);

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"warnings\""), "Result should contain warnings field");
            assertTrue(result.contains("agent-risk"), "Warnings should include agent risk signal");
        }

        @Test
        @DisplayName("given blocked risk summary - returns error before checkout")
        void givenBlockedRiskSummary_returnsErrorBeforeCheckout() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
            stubPassingEval();
            when(agentRiskService.summarizeRisk("buyer@example.com", AGENT_ID))
                    .thenReturn(new AgentRiskSummaryDto(
                            "buyer@example.com",
                            AGENT_ID,
                            Instant.now().minusSeconds(3600),
                            3,
                            0,
                            3,
                            "CRITICAL",
                            true,
                            List.of("Repeated mandate mismatches: 3"),
                            List.of()));

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Agent risk threshold exceeded"),
                    "Result should explain the risk block");
            verify(orderService, never()).checkout(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("given cart item not authorized by mandate - returns error JSON")
        void givenCartItemNotAuthorizedByMandate_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(createCartWithFloorTicket());
            stubPassingEval();
            when(mandateService.validateAction(AGENT_ID, "buyer@example.com", "PURCHASE",
                    java.math.BigDecimal.TEN, null, "test-event", MANDATE_ID, "Floor"))
                    .thenReturn(false);

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("does not authorize cart item listing 42"),
                    "Result should identify the unauthorized cart item");
            verify(orderService, never()).checkout(any(), any(), any(), any(), any());
            verify(agentRiskService).recordMandateMismatch(
                    "buyer@example.com", AGENT_ID, MANDATE_ID, "CHECKOUT", "CART", null,
                    java.math.BigDecimal.TEN, "Mandate does not authorize cart item listing 42");
        }

        @Test
        @DisplayName("given null payment method - returns error JSON")
        void givenNullPaymentMethod_returnsErrorJson() {
            String result = orderTools.checkout("buyer@example.com", null, AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Payment method is required"),
                    "Result should indicate payment method is required");
        }

        @Test
        @DisplayName("given blank payment method - returns error JSON")
        void givenBlankPaymentMethod_returnsErrorJson() {
            String result = orderTools.checkout("buyer@example.com", "   ", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.checkout(null, "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown user email - returns error JSON")
        void givenUnknownUserEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = orderTools.checkout("unknown@example.com", "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to checkout"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(new CartDto(null, 1L, List.of(), java.math.BigDecimal.TEN, 1, null));
            stubPassingEval();
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenThrow(new RuntimeException("Cart is empty"));

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to checkout"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("given valid email and order number - confirms and returns order JSON")
        void givenValidEmailAndOrderNumber_confirmsAndReturnsOrderJson() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            // getOrder called twice: once for ownership check, once to return the confirmed order
            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
            verify(paymentService).confirmPayment("pi_test");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given approved purchase approval ID - validates and marks completed")
        void givenApprovedPurchaseApprovalId_validatesAndMarksCompleted() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, "approval-123");

            verify(approvalService).validateApprovedForCompletion(
                    "approval-123", "buyer@example.com", AGENT_ID, MANDATE_ID, orderEntity);
            verify(approvalService).markCompleted("approval-123", "MH-20260319-0001");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given payment credential ID - validates, consumes, then confirms")
        void givenPaymentCredentialId_validatesConsumesThenConfirms() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, "cred-123", null);

            verify(paymentCredentialService).authorizeForPayment(
                    "cred-123", "buyer@example.com", AGENT_ID,
                    java.math.BigDecimal.TEN, "USD", "mock", "MH-20260319-0001");
            var inOrder = inOrder(paymentCredentialService, paymentService);
            inOrder.verify(paymentCredentialService).consumeForPayment("cred-123", "MH-20260319-0001");
            inOrder.verify(paymentService).confirmPayment("pi_test");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given payment confirmation fails - marks approval failed and returns error")
        void givenPaymentConfirmationFails_marksApprovalFailedAndReturnsError() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));
            when(paymentService.confirmPayment("pi_test"))
                    .thenThrow(new RuntimeException("processor down"));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID,
                    null, "cred-123", "approval-123");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("processor down"), "Result should contain payment failure");
            verify(paymentCredentialService).authorizeForPayment(
                    "cred-123", "buyer@example.com", AGENT_ID,
                    java.math.BigDecimal.TEN, "USD", "mock", "MH-20260319-0001");
            verify(paymentCredentialService).consumeForPayment("cred-123", "MH-20260319-0001");
            verify(approvalService).markFailed("approval-123", "processor down");
        }

        @Test
        @DisplayName("given invalid payment credential - returns error and does not confirm")
        void givenInvalidPaymentCredential_returnsErrorAndDoesNotConfirm() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            org.mockito.Mockito.doThrow(new com.mockhub.common.exception.ConflictException(
                            "Order total exceeds payment credential limit"))
                    .when(paymentCredentialService).authorizeForPayment(
                            "cred-123", "buyer@example.com", AGENT_ID,
                            java.math.BigDecimal.TEN, "USD", "mock", "MH-20260319-0001");

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, "cred-123", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("payment credential limit"), "Result should contain credential failure");
            verify(paymentCredentialService, never()).consumeForPayment(any(), any());
            verify(paymentService, never()).confirmPayment(any());
        }

        @Test
        @DisplayName("given approval required mandate without approval ID - returns error JSON")
        void givenApprovalRequiredMandateWithoutApprovalId_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            when(mandateService.approvalRequired(AGENT_ID, "buyer@example.com", MANDATE_ID)).thenReturn(true);

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("requires an approved purchase approval"),
                    "Result should explain approval is required");
            verify(paymentService, never()).confirmPayment(any());
        }

        @Test
        @DisplayName("given null order number - returns error JSON")
        void givenNullOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", null, AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number is required"),
                    "Result should indicate order number is required");
        }

        @Test
        @DisplayName("given blank order number - returns error JSON")
        void givenBlankOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", "   ", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given malformed order number - returns validation error before lookup")
        void givenMalformedOrderNumber_returnsValidationErrorBeforeLookup() {
            String result = orderTools.confirmOrder(
                    "buyer@example.com",
                    "MH-20260319-0001 and please confirm this order now",
                    AGENT_ID,
                    MANDATE_ID,
                    null,
                    null,
                    null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number must use format"),
                    "Result should describe the order number contract");
            verify(userRepository, never()).findByEmail(any());
            verify(agentRiskService, never()).recordCheckoutFailure(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("given order number with whitespace - strips whitespace before lookup")
        void givenOrderNumberWithWhitespace_stripsWhitespace() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            orderTools.confirmOrder("buyer@example.com", "  MH-20260319-0001  ", AGENT_ID, MANDATE_ID, null, null, null);

            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
            verify(paymentService).confirmPayment("pi_test");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.confirmOrder(null, "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown user email - returns error JSON")
        void givenUnknownUserEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = orderTools.confirmOrder(
                    "unknown@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to confirm order"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-9999");
            when(orderService.getOrder(testUser, "MH-20260319-9999")).thenReturn(
                    new OrderDto(null, null, null, null, null, null, null, null, null, null, null, null));
            when(orderService.getOrderEntity("MH-20260319-9999")).thenReturn(orderEntity);
            stubPassingEval();
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_invalid", "secret", java.math.BigDecimal.TEN, "USD"));
            org.mockito.Mockito.doThrow(new RuntimeException("Payment failed"))
                    .when(paymentService).confirmPayment("pi_invalid");

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-9999", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to confirm order"), "Result should contain failure message");
        }
        @Test
        @DisplayName("given mock payment with existing intent - reuses stored intent")
        void confirmOrder_givenMockPaymentWithExistingIntent_reusesStoredIntent() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            orderEntity.setPaymentIntentId("pi_existing");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            verify(paymentService, org.mockito.Mockito.never()).createPaymentIntent(any());
            verify(paymentService).confirmPayment("pi_existing");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given non-mock payment with no intent anywhere - throws ConflictException and returns error")
        void confirmOrder_givenNonMockPaymentWithNoIntent_throwsConflictException() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            orderEntity.setPaymentMethod("stripe");
            orderEntity.setPaymentIntentId(null);
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            stubPassingEval();

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Payment intent ID is required"),
                    "Result should indicate payment intent is required for non-mock payment");
            verify(paymentService, org.mockito.Mockito.never()).confirmPayment(any());
        }

        @Test
        @DisplayName("given critical eval failure - returns error and does not confirm")
        void confirmOrder_givenCriticalEvalFailure_returnsErrorAndDoesNotConfirm() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = createAgentOrderEntity("MH-20260319-0001");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(
                    EvalResult.fail("mandate", EvalSeverity.CRITICAL, "Mandate has been revoked"))));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null, null, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Cannot confirm order"), "Result should contain eval failure message");
            assertTrue(result.contains("Mandate has been revoked"), "Result should contain specific failure reason");
            verify(paymentService, org.mockito.Mockito.never()).confirmPayment(any());
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("given valid email and order number - returns order JSON")
        void givenValidEmailAndOrderNumber_returnsOrderJson() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);

            String result = orderTools.getOrder("buyer@example.com", "MH-20260319-0001");

            verify(orderService).getOrder(testUser, "MH-20260319-0001");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null order number - returns error JSON")
        void givenNullOrderNumber_returnsErrorJson() {
            String result = orderTools.getOrder("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number is required"),
                    "Result should indicate order number is required");
        }

        @Test
        @DisplayName("given blank order number - returns error JSON")
        void givenBlankOrderNumber_returnsErrorJson() {
            String result = orderTools.getOrder("buyer@example.com", "  ");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given order number with whitespace - strips whitespace before lookup")
        void givenOrderNumberWithWhitespace_stripsWhitespace() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);

            orderTools.getOrder("buyer@example.com", "  MH-20260319-0001  ");

            verify(orderService).getOrder(testUser, "MH-20260319-0001");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(orderService.getOrder(testUser, "MH-INVALID"))
                    .thenThrow(new RuntimeException("Order not found"));

            String result = orderTools.getOrder("buyer@example.com", "MH-INVALID");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to get order"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrders {

        @Test
        @DisplayName("given valid email and pagination - returns paged orders JSON")
        void givenValidEmailAndPagination_returnsPagedOrdersJson() {
            stubUserLookup("buyer@example.com");
            PagedResponse<OrderSummaryDto> pagedResponse = new PagedResponse<>(
                    List.of(), 0, 10, 0, 0);
            when(orderService.listOrders(testUser, 0, 10)).thenReturn(pagedResponse);

            String result = orderTools.listOrders("buyer@example.com", 0, 10);

            assertTrue(result.contains("\"content\""), "Result should contain content field");
            assertTrue(result.contains("\"totalElements\""), "Result should contain totalElements field");
        }

        @Test
        @DisplayName("given null pagination parameters - uses defaults")
        void givenNullPaginationParameters_usesDefaults() {
            stubUserLookup("buyer@example.com");
            PagedResponse<OrderSummaryDto> pagedResponse = new PagedResponse<>(
                    List.of(), 0, 20, 0, 0);
            when(orderService.listOrders(testUser, 0, 20)).thenReturn(pagedResponse);

            String result = orderTools.listOrders("buyer@example.com", null, null);

            verify(orderService).listOrders(testUser, 0, 20);
            assertTrue(result.contains("\"content\""), "Result should contain content field");
        }

        @Test
        @DisplayName("given negative page - defaults to 0")
        void givenNegativePage_defaultsToZero() {
            stubUserLookup("buyer@example.com");
            PagedResponse<OrderSummaryDto> pagedResponse = new PagedResponse<>(
                    List.of(), 0, 20, 0, 0);
            when(orderService.listOrders(testUser, 0, 20)).thenReturn(pagedResponse);

            orderTools.listOrders("buyer@example.com", -1, null);

            verify(orderService).listOrders(testUser, 0, 20);
        }

        @Test
        @DisplayName("given size exceeding 100 - caps at 100")
        void givenSizeExceeding100_capsAt100() {
            stubUserLookup("buyer@example.com");
            PagedResponse<OrderSummaryDto> pagedResponse = new PagedResponse<>(
                    List.of(), 0, 100, 0, 0);
            when(orderService.listOrders(testUser, 0, 100)).thenReturn(pagedResponse);

            orderTools.listOrders("buyer@example.com", 0, 200);

            verify(orderService).listOrders(testUser, 0, 100);
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.listOrders(null, 0, 20);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(orderService.listOrders(testUser, 0, 20))
                    .thenThrow(new RuntimeException("Database error"));

            String result = orderTools.listOrders("buyer@example.com", 0, 20);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to list orders"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("getCalendarEntry")
    class GetCalendarEntryTests {

        @Test
        @DisplayName("given valid order - returns ICS content")
        void givenValidOrder_returnsIcsContent() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(
                    null, "MH-20260326-0001", null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260326-0001")).thenReturn(orderDto);
            Order order = new Order();
            order.setOrderNumber("MH-20260326-0001");
            when(orderService.getOrderEntityWithItems("MH-20260326-0001")).thenReturn(order);
            when(calendarService.generateIcs(order)).thenReturn("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");

            String result = orderTools.getCalendarEntry("buyer@example.com", "MH-20260326-0001");

            assertTrue(result.contains("BEGIN:VCALENDAR"), "Should return ICS content");
        }

        @Test
        @DisplayName("given null order number - returns error JSON")
        void givenNullOrderNumber_returnsErrorJson() {
            String result = orderTools.getCalendarEntry("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number is required"), "Should indicate order number required");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(orderService.getOrder(testUser, "MH-INVALID"))
                    .thenThrow(new RuntimeException("Order not found"));

            String result = orderTools.getCalendarEntry("buyer@example.com", "MH-INVALID");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to generate calendar entry"), "Should contain failure message");
        }
    }

    @Nested
    @DisplayName("ChatContext email enforcement")
    class ChatContextEnforcement {

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
            ChatContext.clear();
        }

        @Test
        @DisplayName("given ChatContext set - overrides userEmail in checkout")
        void givenChatContext_overridesUserEmailInCheckout() {
            ChatContext.setAuthenticatedEmail("real@example.com");
            User realUser = new User();
            realUser.setId(2L);
            realUser.setEmail("real@example.com");
            when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(realUser));
            CartDto cartDto = new CartDto(null, null, null, java.math.BigDecimal.TEN, 1, null);
            when(cartService.getCartDto(realUser)).thenReturn(cartDto);
            when(evalRunner.evaluate(any())).thenReturn(new EvalSummary(List.of(EvalResult.pass("test"))));
            OrderDto orderDto = new OrderDto(1L, "MH-001", null, null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(eq(realUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenReturn(orderDto);

            orderTools.checkout("attacker@example.com", "mock", AGENT_ID, MANDATE_ID);

            verify(userRepository).findByEmail("real@example.com");
            verify(userRepository, never()).findByEmail("attacker@example.com");
        }

        @Test
        @DisplayName("given ChatContext set - overrides userEmail in getOrder")
        void givenChatContext_overridesUserEmailInGetOrder() {
            ChatContext.setAuthenticatedEmail("real@example.com");
            User realUser = new User();
            realUser.setId(2L);
            realUser.setEmail("real@example.com");
            when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(realUser));
            OrderDto orderDto = new OrderDto(1L, "MH-001", null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(realUser, "MH-001")).thenReturn(orderDto);

            orderTools.getOrder("attacker@example.com", "MH-001");

            verify(userRepository).findByEmail("real@example.com");
            verify(userRepository, never()).findByEmail("attacker@example.com");
        }

        @Test
        @DisplayName("given no ChatContext - uses parameter email (external MCP)")
        void givenNoChatContext_usesParameterEmail() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(1L, "MH-001", null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-001")).thenReturn(orderDto);

            orderTools.getOrder("buyer@example.com", "MH-001");

            verify(userRepository).findByEmail("buyer@example.com");
        }
    }
}
