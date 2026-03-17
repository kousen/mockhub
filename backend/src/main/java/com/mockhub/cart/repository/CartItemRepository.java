package com.mockhub.cart.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.cart.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    void deleteByCartIdAndId(Long cartId, Long id);

    boolean existsByCartIdAndListingId(Long cartId, Long listingId);

    long countByAddedAtAfter(Instant cutoff);
}
