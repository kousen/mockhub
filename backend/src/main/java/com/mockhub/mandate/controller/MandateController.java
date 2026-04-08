package com.mockhub.mandate.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.security.SecurityUser;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.dto.WebCreateMandateRequest;
import com.mockhub.mandate.service.MandateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/my/mandates")
@Tag(name = "Mandates", description = "Agent mandate management")
public class MandateController {

    private final MandateService mandateService;

    public MandateController(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @PostMapping
    @Operation(summary = "Create mandate",
            description = "Create a new agent mandate for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Mandate created")
    public ResponseEntity<MandateDto> createMandate(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody WebCreateMandateRequest request) {
        CreateMandateRequest serviceRequest = new CreateMandateRequest(
                request.agentId(),
                securityUser.getEmail(),
                request.scope(),
                request.maxSpendPerTransaction(),
                request.maxSpendTotal(),
                request.allowedCategories(),
                request.allowedEvents(),
                request.expiresAt()
        );
        MandateDto mandate = mandateService.createMandate(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(mandate);
    }

    @GetMapping
    @Operation(summary = "List my mandates",
            description = "Return all mandates for the authenticated user (active, expired, and revoked)")
    @ApiResponse(responseCode = "200", description = "Mandates returned")
    public ResponseEntity<List<MandateDto>> listMandates(
            @AuthenticationPrincipal SecurityUser securityUser) {
        List<MandateDto> mandates = mandateService.listAllMandates(securityUser.getEmail());
        return ResponseEntity.ok(mandates);
    }

    @DeleteMapping("/{mandateId}")
    @Operation(summary = "Revoke mandate",
            description = "Revoke an agent mandate owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Mandate revoked")
    @ApiResponse(responseCode = "404", description = "Mandate not found")
    @ApiResponse(responseCode = "401", description = "Not the mandate owner")
    public ResponseEntity<Void> revokeMandate(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String mandateId) {
        mandateService.revokeMandate(mandateId, securityUser.getEmail());
        return ResponseEntity.noContent().build();
    }
}
