package com.mockhub.agentapproval.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.dto.DenyAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.dto.WebCreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.auth.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/agent-approvals")
@Tag(name = "Agent Purchase Approvals", description = "Audit trail for agent purchase proposals and approvals")
public class AgentPurchaseApprovalController {

    private final AgentPurchaseApprovalService approvalService;

    public AgentPurchaseApprovalController(AgentPurchaseApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    @Operation(summary = "Create purchase proposal",
            description = "Create an agent purchase proposal for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Proposal created")
    public ResponseEntity<AgentPurchaseApprovalDto> createProposal(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody WebCreateAgentPurchaseApprovalRequest request) {
        CreateAgentPurchaseApprovalRequest serviceRequest = new CreateAgentPurchaseApprovalRequest(
                securityUser.getEmail(),
                request.agentId(),
                request.mandateId(),
                request.proposedOrderSnapshot(),
                request.agentRationale(),
                request.subtotal(),
                request.serviceFee(),
                request.total(),
                request.commercePolicySnapshot(),
                request.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.createProposal(serviceRequest));
    }

    @GetMapping
    @Operation(summary = "List purchase approvals",
            description = "List agent purchase proposals and approval outcomes for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Approvals returned")
    public ResponseEntity<List<AgentPurchaseApprovalDto>> listApprovals(
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(approvalService.listForUser(securityUser.getEmail()));
    }

    @GetMapping("/{approvalId}")
    @Operation(summary = "Get purchase approval", description = "Get one agent purchase approval record")
    @ApiResponse(responseCode = "200", description = "Approval returned")
    @ApiResponse(responseCode = "404", description = "Approval not found")
    public ResponseEntity<AgentPurchaseApprovalDto> getApproval(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String approvalId) {
        return ResponseEntity.ok(approvalService.getForUser(approvalId, securityUser.getEmail()));
    }

    @PostMapping("/{approvalId}/approve")
    @Operation(summary = "Approve purchase proposal", description = "Approve a proposed agent purchase")
    @ApiResponse(responseCode = "200", description = "Proposal approved")
    public ResponseEntity<AgentPurchaseApprovalDto> approve(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String approvalId) {
        return ResponseEntity.ok(approvalService.approve(approvalId, securityUser.getEmail()));
    }

    @PostMapping("/{approvalId}/deny")
    @Operation(summary = "Deny purchase proposal", description = "Deny a proposed agent purchase")
    @ApiResponse(responseCode = "200", description = "Proposal denied")
    public ResponseEntity<AgentPurchaseApprovalDto> deny(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String approvalId,
            @RequestBody(required = false) DenyAgentPurchaseApprovalRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(approvalService.deny(approvalId, securityUser.getEmail(), reason));
    }
}
