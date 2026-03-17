package com.mockhub.ticket.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;
import com.mockhub.event.entity.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "listings")
public class Listing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "listed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listedPrice;

    @Column(name = "computed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal computedPrice;

    @Column(name = "price_multiplier", nullable = false, precision = 5, scale = 3)
    private BigDecimal priceMultiplier;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "listed_at", nullable = false)
    private Instant listedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Listing() {
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public BigDecimal getListedPrice() {
        return listedPrice;
    }

    public void setListedPrice(BigDecimal listedPrice) {
        this.listedPrice = listedPrice;
    }

    public BigDecimal getComputedPrice() {
        return computedPrice;
    }

    public void setComputedPrice(BigDecimal computedPrice) {
        this.computedPrice = computedPrice;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getListedAt() {
        return listedAt;
    }

    public void setListedAt(Instant listedAt) {
        this.listedAt = listedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
