package com.mockhub.payment.controller;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.service.OrderService;
import com.mockhub.payment.dto.PaymentConfirmation;
import com.mockhub.payment.dto.PaymentIntentDto;
import com.mockhub.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private User createTestUser() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setPasswordHash("hash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRoles(Set.of(buyerRole));
        return testUser;
    }

    private SecurityUser createSecurityUser() {
        return new SecurityUser(createTestUser());
    }

    private Order createTestOrder(User owner) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-20260317-ABC123");
        order.setUser(owner);
        order.setTotal(new BigDecimal("150.00"));
        order.setSubtotal(new BigDecimal("135.00"));
        order.setServiceFee(new BigDecimal("15.00"));
        order.setPaymentMethod("stripe");
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-intent - unauthenticated - returns 401")
    void createPaymentIntent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNumber": "ORD-20260317-ABC123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-intent - authenticated owner - returns 201 with payment intent")
    void createPaymentIntent_authenticatedOwner_returns201() throws Exception {
        User testUser = createTestUser();
        Order testOrder = createTestOrder(testUser);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrderEntity("ORD-20260317-ABC123")).thenReturn(testOrder);
        when(paymentService.createPaymentIntent(any(Order.class)))
                .thenReturn(new PaymentIntentDto("pi_test123", "cs_test_secret",
                        new BigDecimal("150.00"), "usd"));

        mockMvc.perform(post("/api/v1/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNumber": "ORD-20260317-ABC123"}
                                """)
                        .with(user(createSecurityUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test123"))
                .andExpect(jsonPath("$.clientSecret").value("cs_test_secret"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("usd"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-intent - non-owner - returns 401")
    void createPaymentIntent_nonOwner_returnsUnauthorized() throws Exception {
        User testUser = createTestUser();

        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("hash");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");

        Order testOrder = createTestOrder(otherUser);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrderEntity("ORD-20260317-ABC123")).thenReturn(testOrder);

        mockMvc.perform(post("/api/v1/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNumber": "ORD-20260317-ABC123"}
                                """)
                        .with(user(createSecurityUser())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-intent - blank order number - returns 400")
    void createPaymentIntent_blankOrderNumber_returns400() throws Exception {
        when(userRepository.findByEmail("buyer@example.com"))
                .thenReturn(Optional.of(createTestUser()));

        mockMvc.perform(post("/api/v1/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNumber": ""}
                                """)
                        .with(user(createSecurityUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/payments/confirm - unauthenticated - returns 401")
    void confirmPayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId": "pi_test123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/confirm - authenticated - returns confirmation")
    void confirmPayment_authenticated_returnsConfirmation() throws Exception {
        when(userRepository.findByEmail("buyer@example.com"))
                .thenReturn(Optional.of(createTestUser()));
        when(paymentService.confirmPayment("pi_test123"))
                .thenReturn(new PaymentConfirmation("pi_test123", "succeeded", "ORD-20260317-ABC123"));

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId": "pi_test123"}
                                """)
                        .with(user(createSecurityUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test123"))
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20260317-ABC123"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/confirm - blank payment intent ID - returns 400")
    void confirmPayment_blankPaymentIntentId_returns400() throws Exception {
        when(userRepository.findByEmail("buyer@example.com"))
                .thenReturn(Optional.of(createTestUser()));

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId": ""}
                                """)
                        .with(user(createSecurityUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook - no auth required - returns 200")
    void handleWebhook_noAuthRequired_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"payment_intent.succeeded\"}")
                        .header("Stripe-Signature", "sig_test"))
                .andExpect(status().isOk());

        verify(paymentService).handleWebhook(any(String.class), eq("sig_test"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook - without signature header - still processes")
    void handleWebhook_withoutSignatureHeader_stillProcesses() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"payment_intent.succeeded\"}"))
                .andExpect(status().isOk());

        verify(paymentService).handleWebhook(any(String.class), eq(null));
    }
}
