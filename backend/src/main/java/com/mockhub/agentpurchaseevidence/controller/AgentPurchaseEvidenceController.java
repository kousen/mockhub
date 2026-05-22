package com.mockhub.agentpurchaseevidence.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentpurchaseevidence.service.AgentPurchaseEvidenceService;
import com.mockhub.auth.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Agent Purchase Evidence", description = "Read-only evidence trails for agent-initiated orders")
public class AgentPurchaseEvidenceController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AgentPurchaseEvidenceService evidenceService;

    public AgentPurchaseEvidenceController(AgentPurchaseEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/{orderNumber}/agent-evidence")
    @Operation(summary = "Get agent purchase evidence",
            description = "Return the mandate, approval, payment credential, risk, checkout, and fulfillment "
                    + "evidence associated with an agent-initiated order. Only the order owner or an admin "
                    + "can view it.")
    @ApiResponse(responseCode = "200", description = "Evidence returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated or authenticated user does not own the order")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<AgentPurchaseEvidenceDto> getEvidence(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(evidenceService.getEvidence(
                orderNumber,
                securityUser.getEmail(),
                isAdmin(securityUser)));
    }

    private boolean isAdmin(SecurityUser securityUser) {
        return securityUser.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }
}
