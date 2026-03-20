package com.mockhub.cart.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.entity.Cart;
import com.mockhub.cart.entity.CartItem;
import com.mockhub.cart.repository.CartItemRepository;
import com.mockhub.cart.repository.CartRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.venue.entity.Section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRoles(Set.of(buyerRole));

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
        testCart.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));

        Event testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");

        Section testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Floor");

        Ticket testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setTicketType("GENERAL_ADMISSION");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("LISTED");

        testListing = new Listing();
        testListing.setId(1L);
        testListing.setTicket(testTicket);
        testListing.setEvent(testEvent);
        testListing.setListedPrice(new BigDecimal("75.00"));
        testListing.setComputedPrice(new BigDecimal("75.00"));
        testListing.setPriceMultiplier(BigDecimal.ONE);
        testListing.setStatus("ACTIVE");
        testListing.setListedAt(Instant.now());
    }

    @Test
    @DisplayName("getCartDto - given user with empty cart - returns empty cart DTO")
    void getCartDto_givenUserWithEmptyCart_returnsEmptyCartDto() {
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        CartDto result = cartService.getCartDto(testUser);

        assertNotNull(result, "Cart DTO should not be null");
        assertEquals(0, result.itemCount(), "Cart should be empty");
        assertEquals(BigDecimal.ZERO, result.subtotal(), "Subtotal should be zero");
    }

    @Test
    @DisplayName("getCartDto - given user without cart - returns empty cart DTO")
    void getCartDto_givenUserWithoutCart_returnsEmptyCartDto() {
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

        CartDto result = cartService.getCartDto(testUser);

        assertNotNull(result, "Cart DTO should not be null");
        assertEquals(0, result.itemCount(), "Cart should be empty");
    }

    @Test
    @DisplayName("getCartDto - given expired cart - returns empty cart DTO")
    void getCartDto_givenExpiredCart_returnsEmptyCartDto() {
        testCart.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        CartDto result = cartService.getCartDto(testUser);

        assertNotNull(result, "Cart DTO should not be null");
        assertEquals(0, result.itemCount(), "Expired cart should appear empty");
    }

    @Test
    @DisplayName("addToCart - given active listing - adds item to cart")
    void addToCart_givenActiveListing_addsItemToCart() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.existsByCartIdAndListingId(1L, 1L)).thenReturn(false);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartDto result = cartService.addToCart(testUser, 1L);

        assertNotNull(result, "Cart DTO should not be null");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("addToCart - given inactive listing - throws ConflictException")
    void addToCart_givenInactiveListing_throwsConflictException() {
        testListing.setStatus("SOLD");
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));

        assertThrows(ConflictException.class,
                () -> cartService.addToCart(testUser, 1L),
                "Should throw ConflictException for inactive listing");
    }

    @Test
    @DisplayName("addToCart - given duplicate listing - throws ConflictException")
    void addToCart_givenDuplicateListing_throwsConflictException() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.existsByCartIdAndListingId(1L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> cartService.addToCart(testUser, 1L),
                "Should throw ConflictException for duplicate listing in cart");
    }

    @Test
    @DisplayName("addToCart - given nonexistent listing - throws ResourceNotFoundException")
    void addToCart_givenNonexistentListing_throwsResourceNotFoundException() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addToCart(testUser, 999L),
                "Should throw ResourceNotFoundException for unknown listing");
    }

    @Test
    @DisplayName("removeFromCart - given existing cart item - removes it")
    void removeFromCart_givenExistingCartItem_removesIt() {
        CartItem cartItem = new CartItem();
        cartItem.setId(10L);
        cartItem.setCart(testCart);
        cartItem.setListing(testListing);
        cartItem.setPriceAtAdd(new BigDecimal("75.00"));
        cartItem.setAddedAt(Instant.now());
        testCart.getItems().add(cartItem);

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartDto result = cartService.removeFromCart(testUser, 10L);

        assertNotNull(result, "Cart DTO should not be null");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("removeFromCart - given nonexistent cart item - throws ResourceNotFoundException")
    void removeFromCart_givenNonexistentCartItem_throwsResourceNotFoundException() {
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeFromCart(testUser, 999L),
                "Should throw ResourceNotFoundException for unknown cart item");
    }

    @Test
    @DisplayName("clearCart - given user with cart - clears all items")
    void clearCart_givenUserWithCart_clearsAllItems() {
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        cartService.clearCart(testUser);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("clearCart - given user without cart - does nothing")
    void clearCart_givenUserWithoutCart_doesNothing() {
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

        cartService.clearCart(testUser);

        verify(cartRepository, never()).save(any(Cart.class));
    }
}
