package com.mockhub.ticket.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.security.SecurityUser;
import com.mockhub.ticket.dto.EarningsSummaryDto;
import com.mockhub.ticket.dto.SellListingRequest;
import com.mockhub.ticket.dto.SellerListingDto;
import com.mockhub.ticket.dto.UpdatePriceRequest;
import com.mockhub.ticket.service.ListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Seller", description = "Seller listing and earnings management")
public class SellerController {

    private final ListingService listingService;

    public SellerController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping("/listings")
    @Operation(summary = "Create seller listing",
            description = "List a ticket for sale on the marketplace")
    @ApiResponse(responseCode = "201", description = "Listing created")
    @ApiResponse(responseCode = "404", description = "Event or ticket not found")
    @ApiResponse(responseCode = "409", description = "Ticket not available or already listed")
    public ResponseEntity<SellerListingDto> createSellerListing(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody SellListingRequest request) {
        SellerListingDto listing = listingService.createSellerListing(
                securityUser.getEmail(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(listing);
    }

    @GetMapping("/my/listings")
    @Operation(summary = "Get my listings",
            description = "Return the authenticated seller's listings")
    @ApiResponse(responseCode = "200", description = "Listings returned")
    public ResponseEntity<List<SellerListingDto>> getSellerListings(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(required = false) String status) {
        List<SellerListingDto> listings = listingService.getSellerListings(
                securityUser.getEmail(), status);
        return ResponseEntity.ok(listings);
    }

    @PutMapping("/listings/{id}/price")
    @Operation(summary = "Update listing price",
            description = "Update the listed price for a seller-owned listing")
    @ApiResponse(responseCode = "200", description = "Price updated")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    @ApiResponse(responseCode = "401", description = "Not the listing owner")
    public ResponseEntity<SellerListingDto> updateListingPrice(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriceRequest request) {
        SellerListingDto listing = listingService.updateListingPrice(
                id, securityUser.getEmail(), request);
        return ResponseEntity.ok(listing);
    }

    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Deactivate listing",
            description = "Cancel a seller-owned listing and return ticket to available")
    @ApiResponse(responseCode = "204", description = "Listing deactivated")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    @ApiResponse(responseCode = "401", description = "Not the listing owner")
    public ResponseEntity<Void> deactivateListing(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long id) {
        listingService.deactivateListing(id, securityUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my/earnings")
    @Operation(summary = "Get earnings summary",
            description = "Return the authenticated seller's earnings summary")
    @ApiResponse(responseCode = "200", description = "Earnings summary returned")
    public ResponseEntity<EarningsSummaryDto> getEarningsSummary(
            @AuthenticationPrincipal SecurityUser securityUser) {
        EarningsSummaryDto summary = listingService.getEarningsSummary(
                securityUser.getEmail());
        return ResponseEntity.ok(summary);
    }
}
