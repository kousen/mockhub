package com.mockhub.mandate.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mandates")
public class Mandate extends BaseEntity {

    @Column(name = "mandate_id", nullable = false, unique = true, length = 36)
    private String mandateId;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "max_spend_per_transaction", precision = 12, scale = 2)
    private BigDecimal maxSpendPerTransaction;

    @Column(name = "max_spend_total", precision = 12, scale = 2)
    private BigDecimal maxSpendTotal;

    @Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "allowed_categories", length = 1000)
    private String allowedCategories;

    @Column(name = "allowed_events", length = 1000)
    private String allowedEvents;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public Mandate() {
    }

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public BigDecimal getMaxSpendPerTransaction() {
        return maxSpendPerTransaction;
    }

    public void setMaxSpendPerTransaction(BigDecimal maxSpendPerTransaction) {
        this.maxSpendPerTransaction = maxSpendPerTransaction;
    }

    public BigDecimal getMaxSpendTotal() {
        return maxSpendTotal;
    }

    public void setMaxSpendTotal(BigDecimal maxSpendTotal) {
        this.maxSpendTotal = maxSpendTotal;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getAllowedCategories() {
        return allowedCategories;
    }

    public void setAllowedCategories(String allowedCategories) {
        this.allowedCategories = allowedCategories;
    }

    public String getAllowedEvents() {
        return allowedEvents;
    }

    public void setAllowedEvents(String allowedEvents) {
        this.allowedEvents = allowedEvents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
