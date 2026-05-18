package com.mockhub.buyerpreference.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.security.SecurityUser;
import com.mockhub.buyerpreference.dto.BuyerPreferencesDto;
import com.mockhub.buyerpreference.dto.UpdateBuyerPreferencesRequest;
import com.mockhub.buyerpreference.service.BuyerPreferenceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/preferences")
@Tag(name = "Buyer Preferences", description = "Ticket-shopping preferences for users and agents")
public class BuyerPreferenceController {

    private final BuyerPreferenceService buyerPreferenceService;

    public BuyerPreferenceController(BuyerPreferenceService buyerPreferenceService) {
        this.buyerPreferenceService = buyerPreferenceService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's ticket-shopping preferences")
    @ApiResponse(responseCode = "200", description = "Preferences returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<BuyerPreferencesDto> getMyPreferences(
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(buyerPreferenceService.getPreferences(securityUser.getEmail()));
    }

    @PutMapping("/me")
    @Operation(summary = "Replace current user's ticket-shopping preferences")
    @ApiResponse(responseCode = "200", description = "Preferences updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<BuyerPreferencesDto> updateMyPreferences(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateBuyerPreferencesRequest request) {
        return ResponseEntity.ok(buyerPreferenceService.updatePreferences(securityUser.getEmail(), request));
    }
}
