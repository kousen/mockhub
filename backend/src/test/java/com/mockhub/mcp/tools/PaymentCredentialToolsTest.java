package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.service.PaymentCredentialService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCredentialToolsTest {

    @Mock
    private PaymentCredentialService paymentCredentialService;

    private PaymentCredentialTools paymentCredentialTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        paymentCredentialTools = new PaymentCredentialTools(paymentCredentialService, objectMapper);
    }

    @Test
    @DisplayName("issuePaymentCredential - valid parameters - returns credential JSON")
    void issuePaymentCredential_givenValidParameters_returnsCredentialJson() {
        PaymentCredentialDto dto = credentialDto("cred-123", "ACTIVE");
        when(paymentCredentialService.issueCredential(any(CreatePaymentCredentialRequest.class)))
                .thenReturn(dto);

        String result = paymentCredentialTools.issuePaymentCredential(
                "buyer@example.com", "agent-1", new BigDecimal("100.00"),
                null, null, null, null);

        assertTrue(result.contains("\"credentialId\":\"cred-123\""),
                "Result should contain credential ID");
        assertTrue(result.contains("\"status\":\"ACTIVE\""),
                "Result should contain status");
        verify(paymentCredentialService).issueCredential(any(CreatePaymentCredentialRequest.class));
    }

    @Test
    @DisplayName("issuePaymentCredential - expiresAt string - parses timestamp")
    void issuePaymentCredential_givenExpiresAt_parsesTimestamp() {
        PaymentCredentialDto dto = credentialDto("cred-123", "ACTIVE");
        when(paymentCredentialService.issueCredential(any(CreatePaymentCredentialRequest.class)))
                .thenReturn(dto);

        String result = paymentCredentialTools.issuePaymentCredential(
                "buyer@example.com", "agent-1", new BigDecimal("100.00"),
                "USD", "ONE_TIME", "mock", "2026-12-31T23:59:59Z");

        assertTrue(result.contains("\"credentialId\":\"cred-123\""),
                "Result should contain credential ID");
        verify(paymentCredentialService).issueCredential(any(CreatePaymentCredentialRequest.class));
    }

    @Test
    @DisplayName("issuePaymentCredential - invalid expiresAt - returns error JSON")
    void issuePaymentCredential_givenInvalidExpiresAt_returnsErrorJson() {
        String result = paymentCredentialTools.issuePaymentCredential(
                "buyer@example.com", "agent-1", new BigDecimal("100.00"),
                "USD", "ONE_TIME", "mock", "not-a-date");

        assertTrue(result.contains("\"error\""), "Result should contain error");
        assertTrue(result.contains("Failed to issue payment credential"),
                "Result should contain failure message");
    }

    @Test
    @DisplayName("listPaymentCredentials - user with credentials - returns JSON array")
    void listPaymentCredentials_givenUserWithCredentials_returnsJsonArray() {
        when(paymentCredentialService.listCredentials("buyer@example.com"))
                .thenReturn(List.of(credentialDto("cred-1", "ACTIVE"), credentialDto("cred-2", "CONSUMED")));

        String result = paymentCredentialTools.listPaymentCredentials("buyer@example.com");

        assertTrue(result.startsWith("["), "Result should be array JSON");
        assertTrue(result.contains("\"cred-1\""), "Result should contain first credential");
        assertTrue(result.contains("\"cred-2\""), "Result should contain second credential");
        verify(paymentCredentialService).listCredentials("buyer@example.com");
    }

    @Test
    @DisplayName("revokePaymentCredential - valid credential - returns revoked credential JSON")
    void revokePaymentCredential_givenCredential_returnsRevokedCredentialJson() {
        when(paymentCredentialService.revokeCredential("cred-123", "buyer@example.com"))
                .thenReturn(credentialDto("cred-123", "REVOKED"));

        String result = paymentCredentialTools.revokePaymentCredential("buyer@example.com", "cred-123");

        assertTrue(result.contains("\"credentialId\":\"cred-123\""),
                "Result should contain credential ID");
        assertTrue(result.contains("\"status\":\"REVOKED\""),
                "Result should contain revoked status");
        verify(paymentCredentialService).revokeCredential("cred-123", "buyer@example.com");
    }

    @Test
    @DisplayName("revokePaymentCredential - service failure - returns error JSON")
    void revokePaymentCredential_givenServiceFailure_returnsErrorJson() {
        when(paymentCredentialService.revokeCredential("missing", "buyer@example.com"))
                .thenThrow(new RuntimeException("Credential not found"));

        String result = paymentCredentialTools.revokePaymentCredential("buyer@example.com", "missing");

        assertTrue(result.contains("\"error\""), "Result should contain error");
        assertTrue(result.contains("Failed to revoke payment credential"),
                "Result should contain failure message");
    }

    private PaymentCredentialDto credentialDto(String credentialId, String status) {
        return new PaymentCredentialDto(
                1L,
                credentialId,
                "buyer@example.com",
                "agent-1",
                PaymentCredentialService.MOCKHUB_MERCHANT,
                new BigDecimal("100.00"),
                "USD",
                "ONE_TIME",
                status,
                "mock",
                null,
                null,
                null,
                null,
                null,
                Instant.now());
    }
}
