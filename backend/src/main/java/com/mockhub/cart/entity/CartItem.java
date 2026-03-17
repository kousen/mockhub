package com.mockhub.cart.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mockhub.common.entity.BaseEntity;
import com.mockhub.ticket.entity.Listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "listing_id"})
})
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "price_at_add", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdd;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public CartItem() {
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    public void setPriceAtAdd(BigDecimal priceAtAdd) {
        this.priceAtAdd = priceAtAdd;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
}
