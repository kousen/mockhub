package com.mockhub.cart.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.cart.entity.Cart;
import com.mockhub.cart.entity.CartItem;
import com.mockhub.cart.repository.CartItemRepository;
import com.mockhub.cart.repository.CartRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final int CART_EXPIRATION_MINUTES = 15;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ListingRepository listingRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ListingRepository listingRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.listingRepository = listingRepository;
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    Cart saved = cartRepository.save(cart);
                    log.info("Created new cart for user {}", user.getId());
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public CartDto getCartDto(User user) {
        Cart cart = cartRepository.findByUser(user).orElse(null);

        if (cart == null || isExpired(cart)) {
            if (cart != null) {
                log.info("Cart {} expired for user {}", cart.getId(), user.getId());
            }
            return emptyCartDto(user);
        }

        return toCartDto(cart);
    }

    @Transactional
    public CartDto addToCart(User user, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!"ACTIVE".equals(listing.getStatus())) {
            throw new ConflictException("Listing is not available");
        }

        Cart cart = getOrCreateCart(user);

        // Check if expired — if so, clear items and reuse cart
        if (isExpired(cart)) {
            cart.getItems().clear();
            log.info("Cart {} expired, clearing items for user {}", cart.getId(), user.getId());
        }

        boolean alreadyInCart = cartItemRepository.existsByCartIdAndListingId(cart.getId(), listingId);
        if (alreadyInCart) {
            throw new ConflictException("Listing is already in your cart");
        }

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setListing(listing);
        cartItem.setPriceAtAdd(listing.getComputedPrice());
        cartItem.setAddedAt(Instant.now());

        cart.getItems().add(cartItem);
        cart.setExpiresAt(Instant.now().plus(CART_EXPIRATION_MINUTES, ChronoUnit.MINUTES));

        cartRepository.save(cart);
        log.info("Added listing {} to cart {} for user {}", listingId, cart.getId(), user.getId());

        return toCartDto(cart);
    }

    @Transactional
    public CartDto removeFromCart(User user, Long cartItemId) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user", user.getId()));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        if (!removed) {
            throw new ResourceNotFoundException("CartItem", "id", cartItemId);
        }

        cartRepository.save(cart);
        log.info("Removed cart item {} from cart {} for user {}", cartItemId, cart.getId(), user.getId());

        return toCartDto(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cart.setExpiresAt(null);
            cartRepository.save(cart);
            log.info("Cleared cart {} for user {}", cart.getId(), user.getId());
        }
    }

    @Transactional
    public CartDto refreshCart(User user) {
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return emptyCartDto(user);
        }
        if (isExpired(cart)) {
            cart.getItems().clear();
            cart.setExpiresAt(null);
            cartRepository.save(cart);
            return emptyCartDto(user);
        }
        cart.setExpiresAt(Instant.now().plus(CART_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
        cartRepository.save(cart);
        log.info("Refreshed cart {} expiration for user {}", cart.getId(), user.getId());
        return toCartDto(cart);
    }

    private boolean isExpired(Cart cart) {
        return cart.getExpiresAt() != null && Instant.now().isAfter(cart.getExpiresAt());
    }

    private CartDto emptyCartDto(User user) {
        return new CartDto(
                null,
                user.getId(),
                List.of(),
                BigDecimal.ZERO,
                0,
                null
        );
    }

    private CartDto toCartDto(Cart cart) {
        List<CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Listing listing = item.getListing();
            Ticket ticket = listing.getTicket();

            String rowLabel = null;
            String seatNumber = null;
            if (ticket.getSeat() != null) {
                rowLabel = ticket.getSeat().getRow().getRowLabel();
                seatNumber = ticket.getSeat().getSeatNumber();
            }

            CartItemDto itemDto = new CartItemDto(
                    item.getId(),
                    listing.getId(),
                    listing.getEvent().getName(),
                    listing.getEvent().getSlug(),
                    ticket.getSection().getName(),
                    rowLabel,
                    seatNumber,
                    ticket.getTicketType(),
                    item.getPriceAtAdd(),
                    listing.getComputedPrice(),
                    item.getAddedAt()
            );
            itemDtos.add(itemDto);
            subtotal = subtotal.add(listing.getComputedPrice());
        }

        return new CartDto(
                cart.getId(),
                cart.getUser().getId(),
                itemDtos,
                subtotal,
                itemDtos.size(),
                cart.getExpiresAt()
        );
    }
}
