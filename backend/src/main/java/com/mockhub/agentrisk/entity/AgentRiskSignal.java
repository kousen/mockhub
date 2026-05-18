package com.mockhub.agentrisk.entity;

import java.math.BigDecimal;

import com.mockhub.common.entity.BaseEntity;
import com.mockhub.eval.dto.EvalSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_risk_signals")
public class AgentRiskSignal extends BaseEntity {

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 40)
    private AgentRiskSignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private EvalSeverity severity;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(name = "mandate_id", length = 100)
    private String mandateId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    public AgentRiskSignal() {
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

    public AgentRiskSignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(AgentRiskSignalType signalType) {
        this.signalType = signalType;
    }

    public EvalSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EvalSeverity severity) {
        this.severity = severity;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
