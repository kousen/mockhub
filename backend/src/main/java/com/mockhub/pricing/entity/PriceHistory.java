package com.mockhub.pricing.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;
import com.mockhub.event.entity.Event;
import com.mockhub.ticket.entity.Listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_history")
public class PriceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "multiplier", nullable = false, precision = 5, scale = 3)
    private BigDecimal multiplier;

    @Column(name = "supply_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal supplyRatio;

    @Column(name = "demand_score", precision = 5, scale = 4)
    private BigDecimal demandScore;

    @Column(name = "days_to_event", nullable = false)
    private int daysToEvent;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public PriceHistory() {
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal getSupplyRatio() {
        return supplyRatio;
    }

    public void setSupplyRatio(BigDecimal supplyRatio) {
        this.supplyRatio = supplyRatio;
    }

    public BigDecimal getDemandScore() {
        return demandScore;
    }

    public void setDemandScore(BigDecimal demandScore) {
        this.demandScore = demandScore;
    }

    public int getDaysToEvent() {
        return daysToEvent;
    }

    public void setDaysToEvent(int daysToEvent) {
        this.daysToEvent = daysToEvent;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
