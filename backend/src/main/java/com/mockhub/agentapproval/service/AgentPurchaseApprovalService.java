package com.mockhub.agentapproval.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.entity.AgentPurchaseApproval;
import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.order.entity.Order;

@Service
public class AgentPurchaseApprovalService {

    private static final String APPROVAL_RESOURCE = "AgentPurchaseApproval";
    private static final String APPROVAL_ID_FIELD = "approvalId";

    private final AgentPurchaseApprovalRepository approvalRepository;

    public AgentPurchaseApprovalService(AgentPurchaseApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    @Transactional
    public AgentPurchaseApprovalDto createProposal(CreateAgentPurchaseApprovalRequest request) {
        AgentPurchaseApproval approval = new AgentPurchaseApproval();
        approval.setApprovalId(UUID.randomUUID().toString());
        approval.setUserEmail(request.userEmail().strip());
        approval.setAgentId(request.agentId().strip());
        approval.setMandateId(request.mandateId().strip());
        approval.setStatus(AgentPurchaseApprovalStatus.PROPOSED);
        approval.setProposedOrderSnapshot(request.proposedOrderSnapshot());
        approval.setAgentRationale(normalize(request.agentRationale()));
        approval.setSubtotal(request.subtotal());
        approval.setServiceFee(request.serviceFee());
        approval.setTotal(request.total());
        approval.setCommercePolicySnapshot(normalize(request.commercePolicySnapshot()));
        approval.setProposedAt(Instant.now());
        approval.setExpiresAt(request.expiresAt());
        return toDto(approvalRepository.save(approval));
    }

    @Transactional(readOnly = true)
    public List<AgentPurchaseApprovalDto> listForUser(String userEmail) {
        return approvalRepository.findByUserEmailOrderByCreatedAtDesc(userEmail.strip()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentPurchaseApprovalDto getForUser(String approvalId, String userEmail) {
        AgentPurchaseApproval approval = getApproval(approvalId);
        verifyOwner(approval, userEmail);
        return toDto(approval);
    }

    @Transactional
    public AgentPurchaseApprovalDto approve(String approvalId, String userEmail) {
        AgentPurchaseApproval approval = getApprovalForUpdate(approvalId);
        verifyOwner(approval, userEmail);
        ensureStatus(approval, AgentPurchaseApprovalStatus.PROPOSED, "approve");
        ensureNotExpired(approval);
        approval.setStatus(AgentPurchaseApprovalStatus.APPROVED);
        approval.setApprovedAt(Instant.now());
        return toDto(approvalRepository.save(approval));
    }

    @Transactional
    public AgentPurchaseApprovalDto deny(String approvalId, String userEmail, String reason) {
        AgentPurchaseApproval approval = getApprovalForUpdate(approvalId);
        verifyOwner(approval, userEmail);
        ensureStatus(approval, AgentPurchaseApprovalStatus.PROPOSED, "deny");
        approval.setStatus(AgentPurchaseApprovalStatus.DENIED);
        approval.setDeniedAt(Instant.now());
        approval.setDenialReason(normalize(reason));
        return toDto(approvalRepository.save(approval));
    }

    @Transactional
    public void validateApprovedForCompletion(String approvalId, String userEmail,
                                              String agentId, String mandateId, Order order) {
        if (approvalId == null || approvalId.isBlank()) {
            return;
        }

        AgentPurchaseApproval approval = getApprovalForUpdate(approvalId);
        verifyOwner(approval, userEmail);
        ensureNotExpired(approval);

        if (approval.getStatus() != AgentPurchaseApprovalStatus.APPROVED) {
            throw new ConflictException("Purchase approval " + approvalId + " is " + approval.getStatus());
        }
        if (!agentId.strip().equals(approval.getAgentId())) {
            throw new ConflictException("Approval agent does not match the checkout agent");
        }
        if (!mandateId.strip().equals(approval.getMandateId())) {
            throw new ConflictException("Approval mandate does not match the checkout mandate");
        }
        if (order.getTotal() != null && approval.getTotal().compareTo(order.getTotal()) != 0) {
            throw new ConflictException("Approval total does not match the checkout total");
        }
    }

    @Transactional
    public void markCompleted(String approvalId, String orderNumber) {
        if (approvalId == null || approvalId.isBlank()) {
            return;
        }

        AgentPurchaseApproval approval = getApprovalForUpdate(approvalId);
        if (approval.getStatus() == AgentPurchaseApprovalStatus.COMPLETED) {
            return;
        }
        ensureStatus(approval, AgentPurchaseApprovalStatus.APPROVED, "complete");
        approval.setStatus(AgentPurchaseApprovalStatus.COMPLETED);
        approval.setCompletedAt(Instant.now());
        approval.setFinalOrderNumber(orderNumber);
        approvalRepository.save(approval);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String approvalId, String reason) {
        if (approvalId == null || approvalId.isBlank()) {
            return;
        }

        AgentPurchaseApproval approval = getApprovalForUpdate(approvalId);
        if (approval.getStatus() == AgentPurchaseApprovalStatus.COMPLETED) {
            return;
        }
        approval.setStatus(AgentPurchaseApprovalStatus.FAILED);
        approval.setFailedAt(Instant.now());
        approval.setFailureReason(normalize(reason));
        approvalRepository.save(approval);
    }

    private AgentPurchaseApproval getApproval(String approvalId) {
        return approvalRepository.findByApprovalId(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        APPROVAL_RESOURCE, APPROVAL_ID_FIELD, approvalId));
    }

    private AgentPurchaseApproval getApprovalForUpdate(String approvalId) {
        return approvalRepository.findByApprovalIdForUpdate(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        APPROVAL_RESOURCE, APPROVAL_ID_FIELD, approvalId));
    }

    private void verifyOwner(AgentPurchaseApproval approval, String userEmail) {
        if (!approval.getUserEmail().equals(userEmail.strip())) {
            throw new UnauthorizedException("You do not own this purchase approval");
        }
    }

    private void ensureStatus(AgentPurchaseApproval approval,
                              AgentPurchaseApprovalStatus expectedStatus, String action) {
        if (approval.getStatus() != expectedStatus) {
            throw new ConflictException("Cannot " + action + " " + approval.getStatus()
                    .name().toLowerCase() + " purchase approval " + approval.getApprovalId());
        }
    }

    private void ensureNotExpired(AgentPurchaseApproval approval) {
        Instant expiresAt = approval.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            // The durable PROPOSED -> EXPIRED transition is handled by LifecycleCleanupService.
            // Persisting it here would be discarded by the rollback this ConflictException triggers
            // (and the rejection is timestamp-based, so it holds regardless of the stored status).
            throw new ConflictException("Purchase approval " + approval.getApprovalId() + " is expired");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private AgentPurchaseApprovalDto toDto(AgentPurchaseApproval approval) {
        return new AgentPurchaseApprovalDto(
                approval.getApprovalId(),
                approval.getUserEmail(),
                approval.getAgentId(),
                approval.getMandateId(),
                approval.getStatus().name(),
                approval.getProposedOrderSnapshot(),
                approval.getAgentRationale(),
                approval.getSubtotal(),
                approval.getServiceFee(),
                approval.getTotal(),
                approval.getCommercePolicySnapshot(),
                approval.getProposedAt(),
                approval.getApprovedAt(),
                approval.getDeniedAt(),
                approval.getExpiresAt(),
                approval.getCompletedAt(),
                approval.getFailedAt(),
                approval.getFinalOrderNumber(),
                approval.getDenialReason(),
                approval.getFailureReason(),
                approval.getCreatedAt()
        );
    }
}
