package com.mockhub.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.auth.entity.User;
import com.mockhub.cart.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findByUser(User user);
}
