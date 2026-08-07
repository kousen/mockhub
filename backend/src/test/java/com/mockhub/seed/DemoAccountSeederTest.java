package com.mockhub.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.event.entity.Event;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoAccountSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    private DemoAccountSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DemoAccountSeeder(userRepository, roleRepository, passwordEncoder,
                listingRepository, orderRepository, cartService, orderService);
    }

    private User existingUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setPasswordHash("hash");
        user.getRoles().add(new Role("ROLE_USER"));
        return user;
    }

    private void stubHealthyUsers() {
        User alice = existingUser(1L, "alice@mockhub.com");
        User bob = existingUser(2L, "bob@mockhub.com");
        when(userRepository.findByEmail("alice@mockhub.com")).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail("bob@mockhub.com")).thenReturn(Optional.of(bob));
        lenient().when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
    }

    private Listing listingForEvent(long listingId, long eventId, String eventName) {
        Listing listing = mock(Listing.class);
        Event event = mock(Event.class);
        lenient().when(event.getId()).thenReturn(eventId);
        lenient().when(event.getName()).thenReturn(eventName);
        lenient().when(listing.getEvent()).thenReturn(event);
        lenient().when(listing.getId()).thenReturn(listingId);
        return listing;
    }

    private OrderDto orderDto(String orderNumber) {
        return new OrderDto(1L, orderNumber, "PENDING", BigDecimal.TEN, BigDecimal.ONE,
                new BigDecimal("11.00"), "mock", null, Instant.now(), List.of(), null, null);
    }

    @Test
    @DisplayName("run - given missing demo users - creates alice and bob with ROLE_USER")
    void run_givenMissingDemoUsers_createsAliceAndBob() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
        when(listingRepository.findCheapestActiveListingsForFutureEvents(any(), any()))
                .thenReturn(List.of());
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        List<String> emails = captor.getAllValues().stream().map(User::getEmail).toList();
        assertTrue(emails.contains("alice@mockhub.com"), "Alice should be created");
        assertTrue(emails.contains("bob@mockhub.com"), "Bob should be created");
        captor.getAllValues().forEach(user -> {
            assertTrue(user.isEnabled(), "Demo users should be enabled");
            assertTrue(user.isEmailVerified(), "Demo users should be email-verified");
            assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getName())),
                    "Demo users should have ROLE_USER");
        });
    }

    @Test
    @DisplayName("run - given healthy existing users and seeded orders - saves nothing")
    void run_givenHealthyExistingState_savesNothing() {
        stubHealthyUsers();
        when(listingRepository.findCheapestActiveListingsForFutureEvents(any(), any()))
                .thenReturn(List.of());
        Order seeded = mock(Order.class);
        when(seeded.getItems()).thenReturn(List.of());
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(seeded));

        seeder.run(null);

        verify(userRepository, never()).save(any());
        verify(orderService, never()).checkout(any(), any(), anyString());
    }

    @Test
    @DisplayName("run - given drifted password - resets it to the demo credential")
    void run_givenDriftedPassword_resetsToDemoCredential() {
        User alice = existingUser(1L, "alice@mockhub.com");
        alice.setPasswordHash("student-changed-it");
        User bob = existingUser(2L, "bob@mockhub.com");
        when(userRepository.findByEmail("alice@mockhub.com")).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail("bob@mockhub.com")).thenReturn(Optional.of(bob));
        when(passwordEncoder.matches("alicedemo123", "student-changed-it")).thenReturn(false);
        when(passwordEncoder.matches("bobdemo123", "hash")).thenReturn(true);
        when(passwordEncoder.encode("alicedemo123")).thenReturn("restored-hash");
        when(userRepository.save(alice)).thenReturn(alice);
        when(listingRepository.findCheapestActiveListingsForFutureEvents(any(), any()))
                .thenReturn(List.of());
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        assertEquals("restored-hash", alice.getPasswordHash(),
                "A drifted demo password should be restored on startup");
        verify(userRepository).save(alice);
        verify(userRepository, never()).save(bob);
    }

    @Test
    @DisplayName("run - given available listings - seeds three confirmed orders for bob from distinct events")
    void run_givenAvailableListings_seedsThreeConfirmedOrdersForBob() {
        stubHealthyUsers();
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        // Two listings share event 10 — the second must be skipped in favor of event 30
        List<Listing> candidates = List.of(
                listingForEvent(101L, 10L, "Event Ten"),
                listingForEvent(102L, 10L, "Event Ten"),
                listingForEvent(103L, 20L, "Event Twenty"),
                listingForEvent(104L, 30L, "Event Thirty"));
        when(listingRepository.findCheapestActiveListingsForFutureEvents(any(), any()))
                .thenReturn(candidates);
        when(orderService.checkout(any(User.class), any(CheckoutRequest.class), anyString()))
                .thenReturn(orderDto("MH-1"), orderDto("MH-2"), orderDto("MH-3"));

        seeder.run(null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderService, times(3)).checkout(any(User.class), any(CheckoutRequest.class), keyCaptor.capture());
        assertEquals(List.of("demo-seed-bob-1", "demo-seed-bob-2", "demo-seed-bob-3"),
                keyCaptor.getAllValues(), "Seeded orders should carry demo-seed idempotency keys");
        verify(cartService).addToCart(any(User.class), eq(101L));
        verify(cartService).addToCart(any(User.class), eq(103L));
        verify(cartService).addToCart(any(User.class), eq(104L));
        verify(cartService, never()).addToCart(any(User.class), eq(102L));
        verify(orderService).confirmOrder("MH-1");
        verify(orderService).confirmOrder("MH-2");
        verify(orderService).confirmOrder("MH-3");
    }

    @Test
    @DisplayName("run - given one checkout failure - still seeds the remaining orders")
    void run_givenOneCheckoutFailure_stillSeedsRemainingOrders() {
        stubHealthyUsers();
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        List<Listing> candidates = List.of(
                listingForEvent(101L, 10L, "Event Ten"),
                listingForEvent(103L, 20L, "Event Twenty"),
                listingForEvent(104L, 30L, "Event Thirty"));
        when(listingRepository.findCheapestActiveListingsForFutureEvents(any(), any()))
                .thenReturn(candidates);
        when(orderService.checkout(any(User.class), any(CheckoutRequest.class), eq("demo-seed-bob-1")))
                .thenThrow(new IllegalStateException("listing gone"));
        when(orderService.checkout(any(User.class), any(CheckoutRequest.class), eq("demo-seed-bob-2")))
                .thenReturn(orderDto("MH-2"));
        when(orderService.checkout(any(User.class), any(CheckoutRequest.class), eq("demo-seed-bob-3")))
                .thenReturn(orderDto("MH-3"));

        seeder.run(null);

        verify(orderService).confirmOrder("MH-2");
        verify(orderService).confirmOrder("MH-3");
        verify(orderService, times(3)).checkout(any(User.class), any(CheckoutRequest.class), anyString());
    }

    @Test
    @DisplayName("run - given user repository failure - does not propagate")
    void run_givenUserRepositoryFailure_doesNotPropagate() {
        when(userRepository.findByEmail(anyString())).thenThrow(new IllegalStateException("db down"));

        seeder.run(null);

        verify(orderService, never()).checkout(any(), any(), anyString());
    }
}
