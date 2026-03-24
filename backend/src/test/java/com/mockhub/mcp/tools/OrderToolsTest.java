package com.mockhub.mcp.tools;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final String AGENT_ID = "shopping-agent";
    private static final String MANDATE_ID = "mandate-123";

    private ObjectMapper objectMapper;
    private OrderTools orderTools;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderTools = new OrderTools(orderService, userRepository, cartService,
                evalRunner, paymentService, objectMapper);

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

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("given valid email and payment method - returns order JSON")
        void givenValidEmailAndPaymentMethod_returnsOrderJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenReturn(new CartDto(null, 1L, List.of(), java.math.BigDecimal.TEN, 1, null));
            stubPassingEval();
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID)))
                    .thenReturn(orderDto);

            String result = orderTools.checkout("buyer@example.com", "mock", AGENT_ID, MANDATE_ID);

            verify(orderService).checkout(eq(testUser), any(CheckoutRequest.class), any(), eq(AGENT_ID), eq(MANDATE_ID));
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
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
            Order orderEntity = new Order();
            orderEntity.setOrderNumber("MH-20260319-0001");
            orderEntity.setAgentId(AGENT_ID);
            orderEntity.setMandateId(MANDATE_ID);
            orderEntity.setPaymentMethod("mock");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            String result = orderTools.confirmOrder(
                    "buyer@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null);

            // getOrder called twice: once for ownership check, once to return the confirmed order
            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
            verify(paymentService).confirmPayment("pi_test");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null order number - returns error JSON")
        void givenNullOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", null, AGENT_ID, MANDATE_ID, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number is required"),
                    "Result should indicate order number is required");
        }

        @Test
        @DisplayName("given blank order number - returns error JSON")
        void givenBlankOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", "   ", AGENT_ID, MANDATE_ID, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given order number with whitespace - strips whitespace before lookup")
        void givenOrderNumberWithWhitespace_stripsWhitespace() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = new Order();
            orderEntity.setOrderNumber("MH-20260319-0001");
            orderEntity.setAgentId(AGENT_ID);
            orderEntity.setMandateId(MANDATE_ID);
            orderEntity.setPaymentMethod("mock");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);
            when(orderService.getOrderEntity("MH-20260319-0001")).thenReturn(orderEntity);
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_test", "secret", java.math.BigDecimal.TEN, "USD"));

            orderTools.confirmOrder("buyer@example.com", "  MH-20260319-0001  ", AGENT_ID, MANDATE_ID, null);

            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
            verify(paymentService).confirmPayment("pi_test");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.confirmOrder(null, "MH-20260319-0001", AGENT_ID, MANDATE_ID, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown user email - returns error JSON")
        void givenUnknownUserEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = orderTools.confirmOrder("unknown@example.com", "MH-20260319-0001", AGENT_ID, MANDATE_ID, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to confirm order"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            Order orderEntity = new Order();
            orderEntity.setOrderNumber("MH-INVALID");
            orderEntity.setAgentId(AGENT_ID);
            orderEntity.setMandateId(MANDATE_ID);
            orderEntity.setPaymentMethod("mock");
            when(orderService.getOrder(testUser, "MH-INVALID")).thenReturn(
                    new OrderDto(null, null, null, null, null, null, null, null, null, null));
            when(orderService.getOrderEntity("MH-INVALID")).thenReturn(orderEntity);
            when(paymentService.createPaymentIntent(orderEntity))
                    .thenReturn(new PaymentIntentDto("pi_invalid", "secret", java.math.BigDecimal.TEN, "USD"));
            org.mockito.Mockito.doThrow(new RuntimeException("Payment failed"))
                    .when(paymentService).confirmPayment("pi_invalid");

            String result = orderTools.confirmOrder("buyer@example.com", "MH-INVALID", AGENT_ID, MANDATE_ID, null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to confirm order"), "Result should contain failure message");
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
                    null, null, null, null, null, null, null, null, null, null);
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
                    null, null, null, null, null, null, null, null, null, null);
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
}
