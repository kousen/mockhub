package com.mockhub.mcp.tools;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.service.CartService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartToolsTest {

    @Mock
    private CartService cartService;

    @Mock
    private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private CartTools cartTools;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        cartTools = new CartTools(cartService, userRepository, objectMapper);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
    }

    private void stubUserLookup(String email) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("given valid email - returns cart JSON")
        void givenValidEmail_returnsCartJson() {
            stubUserLookup("buyer@example.com");
            CartDto cartDto = new CartDto(null, null, null, null, 0, null);
            when(cartService.getCartDto(testUser)).thenReturn(cartDto);

            String result = cartTools.getCart("buyer@example.com");

            verify(cartService).getCartDto(testUser);
            // CartDto fields are null so JSON will have null values, but no error
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = cartTools.getCart(null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("User email is required"), "Result should indicate email is required");
        }

        @Test
        @DisplayName("given blank email - returns error JSON")
        void givenBlankEmail_returnsErrorJson() {
            String result = cartTools.getCart("   ");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given unknown email - returns error JSON with not found message")
        void givenUnknownEmail_returnsErrorJson() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            String result = cartTools.getCart("unknown@example.com");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to get cart"), "Result should contain failure message");
        }

        @Test
        @DisplayName("given cart service throws exception - returns error JSON")
        void givenCartServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.getCartDto(testUser)).thenThrow(new RuntimeException("Cart expired"));

            String result = cartTools.getCart("buyer@example.com");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to get cart"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("addToCart")
    class AddToCart {

        @Test
        @DisplayName("given valid email and listing ID - returns updated cart JSON")
        void givenValidEmailAndListingId_returnsUpdatedCartJson() {
            stubUserLookup("buyer@example.com");
            CartDto cartDto = new CartDto(null, null, null, null, 0, null);
            when(cartService.addToCart(testUser, 42L)).thenReturn(cartDto);

            String result = cartTools.addToCart("buyer@example.com", 42L);

            verify(cartService).addToCart(testUser, 42L);
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null listing ID - returns error JSON")
        void givenNullListingId_returnsErrorJson() {
            String result = cartTools.addToCart("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Listing ID is required"), "Result should indicate listing ID is required");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = cartTools.addToCart(null, 42L);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.addToCart(testUser, 42L))
                    .thenThrow(new RuntimeException("Listing already in cart"));

            String result = cartTools.addToCart("buyer@example.com", 42L);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to add to cart"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("removeFromCart")
    class RemoveFromCart {

        @Test
        @DisplayName("given valid email and item ID - returns updated cart JSON")
        void givenValidEmailAndItemId_returnsUpdatedCartJson() {
            stubUserLookup("buyer@example.com");
            CartDto cartDto = new CartDto(null, null, null, null, 0, null);
            when(cartService.removeFromCart(testUser, 10L)).thenReturn(cartDto);

            String result = cartTools.removeFromCart("buyer@example.com", 10L);

            verify(cartService).removeFromCart(testUser, 10L);
            assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        }

        @Test
        @DisplayName("given null item ID - returns error JSON")
        void givenNullItemId_returnsErrorJson() {
            String result = cartTools.removeFromCart("buyer@example.com", null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Cart item ID is required"), "Result should indicate item ID is required");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            when(cartService.removeFromCart(testUser, 10L))
                    .thenThrow(new RuntimeException("Item not in cart"));

            String result = cartTools.removeFromCart("buyer@example.com", 10L);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to remove from cart"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("given valid email - returns success JSON")
        void givenValidEmail_returnsSuccessJson() {
            stubUserLookup("buyer@example.com");

            String result = cartTools.clearCart("buyer@example.com");

            verify(cartService).clearCart(testUser);
            assertTrue(result.contains("\"status\": \"success\""), "Result should contain success status");
            assertTrue(result.contains("Cart cleared"), "Result should contain cart cleared message");
        }

        @Test
        @DisplayName("given null email - returns error JSON")
        void givenNullEmail_returnsErrorJson() {
            String result = cartTools.clearCart(null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            stubUserLookup("buyer@example.com");
            org.mockito.Mockito.doThrow(new RuntimeException("Cart not found"))
                    .when(cartService).clearCart(testUser);

            String result = cartTools.clearCart("buyer@example.com");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to clear cart"), "Result should contain failure message");
        }
    }
}
