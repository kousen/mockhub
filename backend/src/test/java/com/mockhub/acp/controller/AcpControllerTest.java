package com.mockhub.acp.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.acp.AcpApiKeyFilter;
import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpLineItemResponse;
import com.mockhub.acp.dto.AcpPricing;
import com.mockhub.acp.service.AcpCheckoutService;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcpController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AcpApiKeyFilter.class})
@TestPropertySource(properties = "mockhub.mcp.api-key=test-api-key")
class AcpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcpCheckoutService acpCheckoutService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /acp/v1/checkout - missing API key - returns 401")
    void createCheckout_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/acp/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "buyerEmail": "buyer@test.com",
                                    "lineItems": [{"listingId": 1, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /acp/v1/checkout - valid request with valid key - returns 201")
    void createCheckout_validRequestWithValidKey_returns201() throws Exception {
        AcpCheckoutResponse response = createTestResponse("CREATED");

        when(acpCheckoutService.createCheckout(any())).thenReturn(response);

        mockMvc.perform(post("/acp/v1/checkout")
                        .header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "buyerEmail": "buyer@test.com",
                                    "lineItems": [{"listingId": 1, "quantity": 1}],
                                    "paymentMethod": "mock"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkoutId").value("MH-20260323-0001"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.buyerEmail").value("buyer@test.com"));
    }

    @Test
    @DisplayName("POST /acp/v1/checkout - missing buyer email - returns 401 (API key check first)")
    void createCheckout_missingBuyerEmail_returns401() throws Exception {
        mockMvc.perform(post("/acp/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "lineItems": [{"listingId": 1, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acp/v1/checkout/{id} - missing API key - returns 401")
    void getCheckout_missingApiKey_returns401() throws Exception {
        mockMvc.perform(get("/acp/v1/checkout/MH-20260323-0001")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /acp/v1/checkout/{id}/complete - missing API key - returns 401")
    void completeCheckout_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/acp/v1/checkout/MH-20260323-0001/complete")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /acp/v1/checkout/{id}/cancel - missing API key - returns 401")
    void cancelCheckout_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/acp/v1/checkout/MH-20260323-0001/cancel")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /acp/v1/checkout/{id} - missing API key - returns 401")
    void updateCheckout_missingApiKey_returns401() throws Exception {
        mockMvc.perform(put("/acp/v1/checkout/MH-20260323-0001")
                        .header("X-Buyer-Email", "buyer@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "addItems": [{"listingId": 2, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acp/v1/catalog - missing API key - returns 401")
    void getCatalog_missingApiKey_returns401() throws Exception {
        mockMvc.perform(get("/acp/v1/catalog"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acp/v1/checkout/{id} - valid key - returns 200")
    void getCheckout_validKey_returns200() throws Exception {
        AcpCheckoutResponse response = createTestResponse("CREATED");
        when(acpCheckoutService.getCheckout("MH-20260323-0001", "buyer@test.com")).thenReturn(response);

        mockMvc.perform(get("/acp/v1/checkout/MH-20260323-0001")
                        .header("X-API-Key", "test-api-key")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutId").value("MH-20260323-0001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("PUT /acp/v1/checkout/{id} - valid key - returns 200")
    void updateCheckout_validKey_returns200() throws Exception {
        AcpCheckoutResponse response = createTestResponse("CREATED");
        when(acpCheckoutService.updateCheckout(eq("MH-20260323-0001"), any(), eq("buyer@test.com")))
                .thenReturn(response);

        mockMvc.perform(put("/acp/v1/checkout/MH-20260323-0001")
                        .header("X-API-Key", "test-api-key")
                        .header("X-Buyer-Email", "buyer@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "addItems": [{"listingId": 2, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutId").value("MH-20260323-0001"));
    }

    @Test
    @DisplayName("POST /acp/v1/checkout/{id}/complete - valid key - returns 200")
    void completeCheckout_validKey_returns200() throws Exception {
        AcpCheckoutResponse response = createTestResponse("COMPLETED");
        when(acpCheckoutService.completeCheckout("MH-20260323-0001", "buyer@test.com")).thenReturn(response);

        mockMvc.perform(post("/acp/v1/checkout/MH-20260323-0001/complete")
                        .header("X-API-Key", "test-api-key")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /acp/v1/checkout/{id}/cancel - valid key - returns 200")
    void cancelCheckout_validKey_returns200() throws Exception {
        AcpCheckoutResponse response = createTestResponse("CANCELLED");
        when(acpCheckoutService.cancelCheckout("MH-20260323-0001", "buyer@test.com")).thenReturn(response);

        mockMvc.perform(post("/acp/v1/checkout/MH-20260323-0001/cancel")
                        .header("X-API-Key", "test-api-key")
                        .header("X-Buyer-Email", "buyer@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /acp/v1/catalog - valid key - returns 200")
    void getCatalog_validKey_returns200() throws Exception {
        PagedResponse<AcpCatalogItem> catalogResponse = new PagedResponse<>(
                List.of(new AcpCatalogItem(
                        "test-concert", "Test Concert", "Test Concert", "rock",
                        "Test Venue", "NYC", Instant.now(),
                        new BigDecimal("50.00"), new BigDecimal("50.00"), 10,
                        "/events/test-concert")),
                0, 20, 1, 1);
        when(acpCheckoutService.getCatalog(null, null, null, 0, 20)).thenReturn(catalogResponse);

        mockMvc.perform(get("/acp/v1/catalog")
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value("test-concert"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /acp/v1/catalog - valid key with query params - returns 200")
    void getCatalog_validKeyWithQueryParams_returns200() throws Exception {
        PagedResponse<AcpCatalogItem> catalogResponse = new PagedResponse<>(List.of(), 0, 10, 0, 0);
        when(acpCheckoutService.getCatalog("rock", "music", "NYC", 0, 10)).thenReturn(catalogResponse);

        mockMvc.perform(get("/acp/v1/catalog")
                        .header("X-API-Key", "test-api-key")
                        .param("query", "rock")
                        .param("category", "music")
                        .param("city", "NYC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("POST /acp/v1/checkout - invalid API key - returns 401")
    void createCheckout_invalidApiKey_returns401() throws Exception {
        mockMvc.perform(post("/acp/v1/checkout")
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "buyerEmail": "buyer@test.com",
                                    "lineItems": [{"listingId": 1, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private AcpCheckoutResponse createTestResponse(String status) {
        List<AcpLineItemResponse> lineItems = List.of(
                new AcpLineItemResponse(1L, "Test Concert", "test-concert",
                        "Floor", "A", "1", new BigDecimal("50.00"), 1)
        );

        AcpPricing pricing = new AcpPricing(
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "USD"
        );

        return new AcpCheckoutResponse(
                "MH-20260323-0001",
                status,
                "buyer@test.com",
                lineItems,
                pricing,
                Instant.now(),
                "COMPLETED".equals(status) ? Instant.now() : null
        );
    }
}
