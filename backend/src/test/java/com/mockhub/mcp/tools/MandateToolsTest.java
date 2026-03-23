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
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.service.MandateService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MandateToolsTest {

    @Mock
    private MandateService mandateService;

    private ObjectMapper objectMapper;
    private MandateTools mandateTools;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mandateTools = new MandateTools(mandateService, objectMapper);
    }

    // --- createMandate ---

    @Test
    @DisplayName("createMandate - given valid parameters - returns mandate JSON")
    void createMandate_givenValidParameters_returnsMandateJson() {
        MandateDto mandateDto = new MandateDto(
                1L, "mandate-123", "agent-1", "user@test.com", "PURCHASE",
                new BigDecimal("100.00"), new BigDecimal("500.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"), null, null, "ACTIVE", null, Instant.now());

        when(mandateService.createMandate(any(CreateMandateRequest.class))).thenReturn(mandateDto);

        String result = mandateTools.createMandate(
                "agent-1", "user@test.com", "PURCHASE",
                new BigDecimal("100.00"), new BigDecimal("500.00"),
                null, null, null);

        assertTrue(result.contains("\"mandateId\":\"mandate-123\""), "Result should contain mandateId");
        assertTrue(result.contains("\"agentId\":\"agent-1\""), "Result should contain agentId");
        assertTrue(result.contains("\"scope\":\"PURCHASE\""), "Result should contain scope");
        verify(mandateService).createMandate(any(CreateMandateRequest.class));
    }

    @Test
    @DisplayName("createMandate - given expiresAt string - parses ISO-8601 timestamp")
    void createMandate_givenExpiresAt_parsesIso8601Timestamp() {
        MandateDto mandateDto = new MandateDto(
                1L, "mandate-456", "agent-1", "user@test.com", "BROWSE",
                null, null, BigDecimal.ZERO, null, null, null, "ACTIVE",
                Instant.parse("2026-12-31T23:59:59Z"), Instant.now());

        when(mandateService.createMandate(any(CreateMandateRequest.class))).thenReturn(mandateDto);

        String result = mandateTools.createMandate(
                "agent-1", "user@test.com", "BROWSE",
                null, null, null, null, "2026-12-31T23:59:59Z");

        assertTrue(result.contains("\"mandateId\":\"mandate-456\""), "Result should contain mandateId");
        verify(mandateService).createMandate(any(CreateMandateRequest.class));
    }

    @Test
    @DisplayName("createMandate - given service throws exception - returns error JSON")
    void createMandate_givenServiceThrowsException_returnsErrorJson() {
        when(mandateService.createMandate(any(CreateMandateRequest.class)))
                .thenThrow(new RuntimeException("User not found"));

        String result = mandateTools.createMandate(
                "agent-1", "nobody@test.com", "PURCHASE",
                null, null, null, null, null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to create mandate"), "Result should contain failure message");
        assertTrue(result.contains("User not found"), "Result should contain original error message");
    }

    @Test
    @DisplayName("createMandate - given invalid expiresAt format - returns error JSON")
    void createMandate_givenInvalidExpiresAtFormat_returnsErrorJson() {
        String result = mandateTools.createMandate(
                "agent-1", "user@test.com", "BROWSE",
                null, null, null, null, "not-a-date");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to create mandate"), "Result should contain failure message");
    }

    @Test
    @DisplayName("createMandate - given categories and events - passes them through")
    void createMandate_givenCategoriesAndEvents_passesThemThrough() {
        MandateDto mandateDto = new MandateDto(
                1L, "mandate-789", "agent-1", "user@test.com", "PURCHASE",
                null, null, BigDecimal.ZERO, null, "rock,jazz", "event-1,event-2",
                "ACTIVE", null, Instant.now());

        when(mandateService.createMandate(any(CreateMandateRequest.class))).thenReturn(mandateDto);

        String result = mandateTools.createMandate(
                "agent-1", "user@test.com", "PURCHASE",
                null, null, "rock,jazz", "event-1,event-2", null);

        assertTrue(result.contains("\"allowedCategories\":\"rock,jazz\""), "Result should contain categories");
        assertTrue(result.contains("\"allowedEvents\":\"event-1,event-2\""), "Result should contain events");
    }

    // --- revokeMandate ---

    @Test
    @DisplayName("revokeMandate - given valid mandateId - returns success JSON")
    void revokeMandate_givenValidMandateId_returnsSuccessJson() {
        String result = mandateTools.revokeMandate("mandate-123");

        assertTrue(result.contains("\"status\": \"success\""), "Result should contain success status");
        assertTrue(result.contains("mandate-123"), "Result should contain mandate ID");
        verify(mandateService).revokeMandate("mandate-123");
    }

    @Test
    @DisplayName("revokeMandate - given service throws exception - returns error JSON")
    void revokeMandate_givenServiceThrowsException_returnsErrorJson() {
        doThrow(new RuntimeException("Mandate not found"))
                .when(mandateService).revokeMandate("nonexistent");

        String result = mandateTools.revokeMandate("nonexistent");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to revoke mandate"), "Result should contain failure message");
        assertTrue(result.contains("Mandate not found"), "Result should contain original error message");
    }

    // --- listMandates ---

    @Test
    @DisplayName("listMandates - given user with mandates - returns JSON array")
    void listMandates_givenUserWithMandates_returnsJsonArray() {
        MandateDto mandate1 = new MandateDto(
                1L, "mandate-1", "agent-1", "user@test.com", "BROWSE",
                null, null, BigDecimal.ZERO, null, null, null, "ACTIVE", null, Instant.now());
        MandateDto mandate2 = new MandateDto(
                2L, "mandate-2", "agent-2", "user@test.com", "PURCHASE",
                new BigDecimal("200.00"), null, BigDecimal.ZERO, null, null, null,
                "ACTIVE", null, Instant.now());

        when(mandateService.listMandates("user@test.com")).thenReturn(List.of(mandate1, mandate2));

        String result = mandateTools.listMandates("user@test.com");

        assertTrue(result.startsWith("["), "Result should be a JSON array");
        assertTrue(result.contains("\"mandate-1\""), "Result should contain first mandate");
        assertTrue(result.contains("\"mandate-2\""), "Result should contain second mandate");
        verify(mandateService).listMandates("user@test.com");
    }

    @Test
    @DisplayName("listMandates - given user with no mandates - returns empty array")
    void listMandates_givenUserWithNoMandates_returnsEmptyArray() {
        when(mandateService.listMandates("user@test.com")).thenReturn(List.of());

        String result = mandateTools.listMandates("user@test.com");

        assertTrue(result.equals("[]"), "Result should be an empty JSON array");
    }

    @Test
    @DisplayName("listMandates - given service throws exception - returns error JSON")
    void listMandates_givenServiceThrowsException_returnsErrorJson() {
        when(mandateService.listMandates("bad@test.com"))
                .thenThrow(new RuntimeException("Database error"));

        String result = mandateTools.listMandates("bad@test.com");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to list mandates"), "Result should contain failure message");
        assertTrue(result.contains("Database error"), "Result should contain original error message");
    }

    // --- validateMandate ---

    @Test
    @DisplayName("validateMandate - given authorized action - returns authorized true")
    void validateMandate_givenAuthorizedAction_returnsAuthorizedTrue() {
        when(mandateService.validateAction("agent-1", "user@test.com", "BROWSE", null, null, null))
                .thenReturn(true);

        String result = mandateTools.validateMandate("agent-1", "user@test.com", "BROWSE", null);

        assertTrue(result.contains("\"authorized\": true"), "Result should contain authorized true");
        assertTrue(result.contains("Action is authorized"), "Result should contain authorized message");
    }

    @Test
    @DisplayName("validateMandate - given unauthorized action - returns authorized false")
    void validateMandate_givenUnauthorizedAction_returnsAuthorizedFalse() {
        when(mandateService.validateAction("agent-1", "user@test.com", "PURCHASE",
                new BigDecimal("1000.00"), null, null)).thenReturn(false);

        String result = mandateTools.validateMandate(
                "agent-1", "user@test.com", "PURCHASE", new BigDecimal("1000.00"));

        assertTrue(result.contains("\"authorized\": false"), "Result should contain authorized false");
        assertTrue(result.contains("not authorized"), "Result should contain not authorized message");
    }

    @Test
    @DisplayName("validateMandate - given service throws exception - returns error JSON")
    void validateMandate_givenServiceThrowsException_returnsErrorJson() {
        when(mandateService.validateAction(eq("agent-1"), eq("bad@test.com"), eq("BROWSE"),
                any(), any(), any())).thenThrow(new RuntimeException("Validation failed"));

        String result = mandateTools.validateMandate("agent-1", "bad@test.com", "BROWSE", null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to validate mandate"), "Result should contain failure message");
        assertTrue(result.contains("Validation failed"), "Result should contain original error message");
    }

    @Test
    @DisplayName("validateMandate - given amount parameter - passes amount to service")
    void validateMandate_givenAmountParameter_passesAmountToService() {
        BigDecimal amount = new BigDecimal("75.00");
        when(mandateService.validateAction("agent-1", "user@test.com", "PURCHASE", amount, null, null))
                .thenReturn(true);

        String result = mandateTools.validateMandate("agent-1", "user@test.com", "PURCHASE", amount);

        assertTrue(result.contains("\"authorized\": true"), "Result should contain authorized true");
        verify(mandateService).validateAction("agent-1", "user@test.com", "PURCHASE", amount, null, null);
    }
}
