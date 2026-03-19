package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ResourceNotFoundException;

@Component
public class CartTools {

    private static final Logger log = LoggerFactory.getLogger(CartTools.class);

    private final CartService cartService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CartTools(CartService cartService,
                     UserRepository userRepository,
                     ObjectMapper objectMapper) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get the current shopping cart for a user by their email address. "
            + "Returns cart items with listing details, prices, item count, and cart expiration time.")
    public String getCart(
            @ToolParam(description = "User's email address", required = true) String userEmail) {
        try {
            User user = resolveUser(userEmail);
            CartDto cart = cartService.getCartDto(user);
            return objectMapper.writeValueAsString(cart);
        } catch (Exception e) {
            log.error("Error getting cart for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to get cart: " + e.getMessage());
        }
    }

    @Tool(description = "Add a ticket listing to a user's shopping cart. "
            + "The listing must be active and not already in the cart. Cart expires after 15 minutes.")
    public String addToCart(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "ID of the listing to add to cart", required = true) Long listingId) {
        try {
            if (listingId == null) {
                return errorJson("Listing ID is required");
            }
            User user = resolveUser(userEmail);
            CartDto cart = cartService.addToCart(user, listingId);
            return objectMapper.writeValueAsString(cart);
        } catch (Exception e) {
            log.error("Error adding listing {} to cart for '{}': {}", listingId, userEmail, e.getMessage(), e);
            return errorJson("Failed to add to cart: " + e.getMessage());
        }
    }

    @Tool(description = "Remove a specific item from a user's shopping cart by cart item ID.")
    public String removeFromCart(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "ID of the cart item to remove", required = true) Long itemId) {
        try {
            if (itemId == null) {
                return errorJson("Cart item ID is required");
            }
            User user = resolveUser(userEmail);
            CartDto cart = cartService.removeFromCart(user, itemId);
            return objectMapper.writeValueAsString(cart);
        } catch (Exception e) {
            log.error("Error removing item {} from cart for '{}': {}", itemId, userEmail, e.getMessage(), e);
            return errorJson("Failed to remove from cart: " + e.getMessage());
        }
    }

    @Tool(description = "Clear all items from a user's shopping cart.")
    public String clearCart(
            @ToolParam(description = "User's email address", required = true) String userEmail) {
        try {
            User user = resolveUser(userEmail);
            cartService.clearCart(user);
            return "{\"status\": \"success\", \"message\": \"Cart cleared\"}";
        } catch (Exception e) {
            log.error("Error clearing cart for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to clear cart: " + e.getMessage());
        }
    }

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        return userRepository.findByEmail(email.strip())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
