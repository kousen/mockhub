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
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.dto.OrderSummaryDto;
import com.mockhub.order.service.OrderService;

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

    private ObjectMapper objectMapper;
    private OrderTools orderTools;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderTools = new OrderTools(orderService, userRepository, objectMapper);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
    }

    private void stubUserLookup(String email) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("given valid email and payment method - returns order JSON")
        void givenValidEmailAndPaymentMethod_returnsOrderJson() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any())).thenReturn(orderDto);

            String result = orderTools.checkout("buyer@example.com", "mock");

            verify(orderService).checkout(eq(testUser), any(CheckoutRequest.class), any());
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null payment method - returns error JSON")
        void givenNullPaymentMethod_returnsErrorJson() {
            String result = orderTools.checkout("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Payment method is required"),
                    "Result should indicate payment method is required");
        }

        @Test
        @DisplayName("given blank payment method - returns error JSON")
        void givenBlankPaymentMethod_returnsErrorJson() {
            String result = orderTools.checkout("buyer@example.com", "   ");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.checkout(null, "mock");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown user email - returns error JSON")
        void givenUnknownUserEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = orderTools.checkout("unknown@example.com", "mock");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to checkout"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(orderService.checkout(eq(testUser), any(CheckoutRequest.class), any()))
                    .thenThrow(new RuntimeException("Cart is empty"));

            String result = orderTools.checkout("buyer@example.com", "mock");

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
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);

            String result = orderTools.confirmOrder("buyer@example.com", "MH-20260319-0001");

            verify(orderService).confirmOrder("MH-20260319-0001");
            // getOrder called twice: once for ownership check, once to return the confirmed order
            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null order number - returns error JSON")
        void givenNullOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Order number is required"),
                    "Result should indicate order number is required");
        }

        @Test
        @DisplayName("given blank order number - returns error JSON")
        void givenBlankOrderNumber_returnsErrorJson() {
            String result = orderTools.confirmOrder("buyer@example.com", "   ");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given order number with whitespace - strips whitespace before lookup")
        void givenOrderNumberWithWhitespace_stripsWhitespace() {
            stubUserLookup("buyer@example.com");
            OrderDto orderDto = new OrderDto(
                    null, null, null, null, null, null, null, null, null, null);
            when(orderService.getOrder(testUser, "MH-20260319-0001")).thenReturn(orderDto);

            orderTools.confirmOrder("buyer@example.com", "  MH-20260319-0001  ");

            verify(orderService).confirmOrder("MH-20260319-0001");
            verify(orderService, times(2)).getOrder(testUser, "MH-20260319-0001");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = orderTools.confirmOrder(null, "MH-20260319-0001");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown user email - returns error JSON")
        void givenUnknownUserEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = orderTools.confirmOrder("unknown@example.com", "MH-20260319-0001");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to confirm order"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            org.mockito.Mockito.doThrow(new RuntimeException("Order not found"))
                    .when(orderService).confirmOrder("MH-INVALID");

            String result = orderTools.confirmOrder("buyer@example.com", "MH-INVALID");

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
