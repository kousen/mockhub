package com.mockhub.paymentcredential.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_credentials")
public class PaymentCredential extends BaseEntity {

    @Column(name = "credential_id", nullable = false, unique = true, length = 36)
    private String credentialId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "allowed_merchant", nullable = false, length = 100)
    private String allowedMerchant;

    @Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private PaymentCredentialUsage usage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentCredentialStatus status;

    @Column(name = "backing_payment_method", nullable = false, length = 30)
    private String backingPaymentMethod;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "consumed_by_order_number", length = 20)
    private String consumedByOrderNumber;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public PaymentCredential() {
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
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

    public String getAllowedMerchant() {
        return allowedMerchant;
    }

    public void setAllowedMerchant(String allowedMerchant) {
        this.allowedMerchant = allowedMerchant;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentCredentialUsage getUsage() {
        return usage;
    }

    public void setUsage(PaymentCredentialUsage usage) {
        this.usage = usage;
    }

    public PaymentCredentialStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentCredentialStatus status) {
        this.status = status;
    }

    public String getBackingPaymentMethod() {
        return backingPaymentMethod;
    }

    public void setBackingPaymentMethod(String backingPaymentMethod) {
        this.backingPaymentMethod = backingPaymentMethod;
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

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public String getConsumedByOrderNumber() {
        return consumedByOrderNumber;
    }

    public void setConsumedByOrderNumber(String consumedByOrderNumber) {
        this.consumedByOrderNumber = consumedByOrderNumber;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
