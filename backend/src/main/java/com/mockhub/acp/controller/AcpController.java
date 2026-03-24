package com.mockhub.acp.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpActionRequest;
import com.mockhub.acp.dto.AcpCheckoutRequest;
import com.mockhub.acp.dto.AcpCheckoutResponse;
import com.mockhub.acp.dto.AcpCompleteRequest;
import com.mockhub.acp.dto.AcpListingItem;
import com.mockhub.acp.dto.AcpUpdateRequest;
import com.mockhub.acp.service.AcpCheckoutService;
import com.mockhub.common.dto.PagedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/acp/v1")
public class AcpController {

    private final AcpCheckoutService acpCheckoutService;

    public AcpController(AcpCheckoutService acpCheckoutService) {
        this.acpCheckoutService = acpCheckoutService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<AcpCheckoutResponse> createCheckout(
            @Valid @RequestBody AcpCheckoutRequest request) {
        AcpCheckoutResponse response = acpCheckoutService.createCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/checkout/{checkoutId}")
    public ResponseEntity<AcpCheckoutResponse> getCheckout(
            @PathVariable String checkoutId,
            @RequestHeader("X-Buyer-Email") String buyerEmail) {
        AcpCheckoutResponse response = acpCheckoutService.getCheckout(checkoutId, buyerEmail);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/checkout/{checkoutId}")
    public ResponseEntity<AcpCheckoutResponse> updateCheckout(
            @PathVariable String checkoutId,
            @Valid @RequestBody AcpUpdateRequest request,
            @RequestHeader("X-Buyer-Email") String buyerEmail) {
        AcpCheckoutResponse response = acpCheckoutService.updateCheckout(checkoutId, request, buyerEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{checkoutId}/complete")
    public ResponseEntity<AcpCheckoutResponse> completeCheckout(
            @PathVariable String checkoutId,
            @RequestHeader("X-Buyer-Email") String buyerEmail,
            @Valid @RequestBody AcpCompleteRequest request) {
        AcpCheckoutResponse response = acpCheckoutService.completeCheckout(checkoutId, buyerEmail, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{checkoutId}/cancel")
    public ResponseEntity<AcpCheckoutResponse> cancelCheckout(
            @PathVariable String checkoutId,
            @RequestHeader("X-Buyer-Email") String buyerEmail,
            @Valid @RequestBody AcpActionRequest request) {
        AcpCheckoutResponse response = acpCheckoutService.cancelCheckout(checkoutId, buyerEmail, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalog")
    public ResponseEntity<PagedResponse<AcpCatalogItem>> getCatalog(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AcpCatalogItem> response = acpCheckoutService.getCatalog(
                query, category, city, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/listings")
    public ResponseEntity<PagedResponse<AcpListingItem>> getListings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AcpListingItem> response = acpCheckoutService.getListings(
                query, category, city, dateFrom, dateTo, minPrice, maxPrice, section, page, size);
        return ResponseEntity.ok(response);
    }
}
