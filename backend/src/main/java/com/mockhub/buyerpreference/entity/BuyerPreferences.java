package com.mockhub.buyerpreference.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mockhub.auth.entity.User;
import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "buyer_preferences")
public class BuyerPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_artists", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> preferredArtists = new ArrayList<>();

    @Column(name = "preferred_categories", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> preferredCategories = new ArrayList<>();

    @Column(name = "preferred_cities", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> preferredCities = new ArrayList<>();

    @Column(name = "preferred_venues", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> preferredVenues = new ArrayList<>();

    @Column(name = "preferred_sections", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> preferredSections = new ArrayList<>();

    @Column(name = "disliked_venues", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> dislikedVenues = new ArrayList<>();

    @Column(name = "disliked_categories", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> dislikedCategories = new ArrayList<>();

    @Column(name = "max_total_price", precision = 12, scale = 2)
    private BigDecimal maxTotalPrice;

    @Column(name = "max_service_fee_percent", precision = 5, scale = 2)
    private BigDecimal maxServiceFeePercent;

    @Column(name = "all_in_price_only", nullable = false)
    private boolean allInPriceOnly;

    @Column(name = "accessibility_needs", length = 1000)
    private String accessibilityNeeds;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tolerance", nullable = false, length = 30)
    private RiskTolerance riskTolerance = RiskTolerance.BEST_VALUE;

    @Column(name = "willing_to_wait_for_price_drops", nullable = false)
    private boolean willingToWaitForPriceDrops;

    public BuyerPreferences() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getPreferredArtists() {
        return preferredArtists;
    }

    public void setPreferredArtists(List<String> preferredArtists) {
        this.preferredArtists = preferredArtists;
    }

    public List<String> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(List<String> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }

    public List<String> getPreferredCities() {
        return preferredCities;
    }

    public void setPreferredCities(List<String> preferredCities) {
        this.preferredCities = preferredCities;
    }

    public List<String> getPreferredVenues() {
        return preferredVenues;
    }

    public void setPreferredVenues(List<String> preferredVenues) {
        this.preferredVenues = preferredVenues;
    }

    public List<String> getPreferredSections() {
        return preferredSections;
    }

    public void setPreferredSections(List<String> preferredSections) {
        this.preferredSections = preferredSections;
    }

    public List<String> getDislikedVenues() {
        return dislikedVenues;
    }

    public void setDislikedVenues(List<String> dislikedVenues) {
        this.dislikedVenues = dislikedVenues;
    }

    public List<String> getDislikedCategories() {
        return dislikedCategories;
    }

    public void setDislikedCategories(List<String> dislikedCategories) {
        this.dislikedCategories = dislikedCategories;
    }

    public BigDecimal getMaxTotalPrice() {
        return maxTotalPrice;
    }

    public void setMaxTotalPrice(BigDecimal maxTotalPrice) {
        this.maxTotalPrice = maxTotalPrice;
    }

    public BigDecimal getMaxServiceFeePercent() {
        return maxServiceFeePercent;
    }

    public void setMaxServiceFeePercent(BigDecimal maxServiceFeePercent) {
        this.maxServiceFeePercent = maxServiceFeePercent;
    }

    public boolean isAllInPriceOnly() {
        return allInPriceOnly;
    }

    public void setAllInPriceOnly(boolean allInPriceOnly) {
        this.allInPriceOnly = allInPriceOnly;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public RiskTolerance getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(RiskTolerance riskTolerance) {
        this.riskTolerance = riskTolerance;
    }

    public boolean isWillingToWaitForPriceDrops() {
        return willingToWaitForPriceDrops;
    }

    public void setWillingToWaitForPriceDrops(boolean willingToWaitForPriceDrops) {
        this.willingToWaitForPriceDrops = willingToWaitForPriceDrops;
    }
}
