package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

@Component
public class CartTools {

    private static final Logger log = LoggerFactory.getLogger(CartTools.class);

    private final CartService cartService;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final EvalRunner evalRunner;
    private final ObjectMapper objectMapper;

    public CartTools(CartService cartService,
                     UserRepository userRepository,
                     ListingRepository listingRepository,
                     EvalRunner evalRunner,
                     ObjectMapper objectMapper) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.evalRunner = evalRunner;
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
            + "The listing must be active and not already in the cart. Cart expires after 15 minutes. "
            + "Returns the full updated cart contents, so a separate getCart call is not needed. "
            + "Autonomous agent actions must include both agentId and mandateId.")
    @Transactional
    public String addToCart(
            @ToolParam(description = "User's email address", required = true) String userEmail,
            @ToolParam(description = "ID of the listing to add to cart", required = true) Long listingId,
            @ToolParam(description = "Agent ID performing the purchase action", required = true) String agentId,
            @ToolParam(description = "Active mandate ID authorizing the purchase action",
                    required = true) String mandateId) {
        try {
            if (listingId == null) {
                return errorJson("Listing ID is required");
            }
            if (agentId == null || agentId.isBlank()) {
                return errorJson("Agent ID is required");
            }
            if (mandateId == null || mandateId.isBlank()) {
                return errorJson("Mandate ID is required");
            }

            java.util.Optional<Listing> listingOpt = listingRepository.findById(listingId);
            if (listingOpt.isPresent()) {
                Listing listing = listingOpt.get();
                String categorySlug = listing.getEvent().getCategory() != null
                        ? listing.getEvent().getCategory().getSlug() : null;
                EvalContext evalContext = EvalContext.forAgentAction(agentId.strip(), userEmail,
                        listing.getEvent(), listing, listing.getComputedPrice(), categorySlug, mandateId.strip());
                EvalSummary evalSummary = evalRunner.evaluate(evalContext);
                if (evalSummary.hasCriticalFailure()) {
                    String failureMessage = evalSummary.failures().stream()
                            .map(r -> r.conditionName() + ": " + r.message())
                            .collect(java.util.stream.Collectors.joining("; "));
                    log.warn("Eval blocked addToCart for listing {}: {}", listingId, failureMessage);
                    return errorJson("Cannot add to cart: " + failureMessage);
                }
            }

            User user = resolveUser(userEmail);
            CartDto cartDto = cartService.addToCart(user, listingId);

            EvalContext cartContext = EvalContext.forCart(cartDto);
            EvalSummary cartEval = evalRunner.evaluate(cartContext);
            if (!cartEval.allPassed()) {
                String warnings = cartEval.failures().stream()
                        .map(r -> r.conditionName() + ": " + r.message())
                        .collect(java.util.stream.Collectors.joining("; "));
                String cartJson = objectMapper.writeValueAsString(cartDto);
                return "{\"cart\": " + cartJson + ", \"warnings\": \"" + warnings.replace("\"", "'") + "\"}";
            }

            return objectMapper.writeValueAsString(cartDto);
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

    @Tool(description = "Refresh the cart expiration timer without modifying contents. "
            + "Useful when an agent needs more time to compare options before completing a purchase. "
            + "Resets the 15-minute TTL. Returns the updated cart with new expiration time. "
            + "If the cart has already expired, returns an empty cart.")
    public String refreshCart(
            @ToolParam(description = "User's email address", required = true) String userEmail) {
        try {
            User user = resolveUser(userEmail);
            CartDto cart = cartService.refreshCart(user);
            return objectMapper.writeValueAsString(cart);
        } catch (Exception e) {
            log.error("Error refreshing cart for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to refresh cart: " + e.getMessage());
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
