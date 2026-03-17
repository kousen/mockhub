package com.mockhub.cart.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.cart.dto.AddToCartRequest;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService,
                          UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal SecurityUser securityUser) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(cartService.getCartDto(user));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addToCart(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AddToCartRequest request) {
        User user = resolveUser(securityUser);
        CartDto cartDto = cartService.addToCart(user, request.listingId());
        return ResponseEntity.status(HttpStatus.CREATED).body(cartDto);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeFromCart(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long itemId) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(cartService.removeFromCart(user, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal SecurityUser securityUser) {
        User user = resolveUser(securityUser);
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(SecurityUser securityUser) {
        return userRepository.findByEmail(securityUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", securityUser.getEmail()));
    }
}
