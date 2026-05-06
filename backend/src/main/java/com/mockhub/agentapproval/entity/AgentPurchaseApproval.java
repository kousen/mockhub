package com.mockhub.agentapproval.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_purchase_approvals")
public class AgentPurchaseApproval extends BaseEntity {

    @Column(name = "approval_id", nullable = false, unique = true, length = 36)
    private String approvalId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "mandate_id", nullable = false, length = 36)
    private String mandateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AgentPurchaseApprovalStatus status;

    @Column(name = "proposed_order_snapshot", nullable = false, columnDefinition = "TEXT")
    private String proposedOrderSnapshot;

    @Column(name = "agent_rationale", columnDefinition = "TEXT")
    private String agentRationale;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "service_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "commerce_policy_snapshot", columnDefinition = "TEXT")
    private String commercePolicySnapshot;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "denied_at")
    private Instant deniedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "final_order_number", length = 20)
    private String finalOrderNumber;

    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public AgentPurchaseApproval() {
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }

    public AgentPurchaseApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(AgentPurchaseApprovalStatus status) {
        this.status = status;
    }

    public String getProposedOrderSnapshot() {
        return proposedOrderSnapshot;
    }

    public void setProposedOrderSnapshot(String proposedOrderSnapshot) {
        this.proposedOrderSnapshot = proposedOrderSnapshot;
    }

    public String getAgentRationale() {
        return agentRationale;
    }

    public void setAgentRationale(String agentRationale) {
        this.agentRationale = agentRationale;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCommercePolicySnapshot() {
        return commercePolicySnapshot;
    }

    public void setCommercePolicySnapshot(String commercePolicySnapshot) {
        this.commercePolicySnapshot = commercePolicySnapshot;
    }

    public Instant getProposedAt() {
        return proposedAt;
    }

    public void setProposedAt(Instant proposedAt) {
        this.proposedAt = proposedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getDeniedAt() {
        return deniedAt;
    }

    public void setDeniedAt(Instant deniedAt) {
        this.deniedAt = deniedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getFinalOrderNumber() {
        return finalOrderNumber;
    }

    public void setFinalOrderNumber(String finalOrderNumber) {
        this.finalOrderNumber = finalOrderNumber;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
