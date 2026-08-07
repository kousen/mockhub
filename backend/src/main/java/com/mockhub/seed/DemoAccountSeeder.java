package com.mockhub.seed;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.cart.service.CartService;
import com.mockhub.order.dto.CheckoutRequest;
import com.mockhub.order.dto.OrderDto;
import com.mockhub.order.entity.Order;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.order.service.OrderService;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

/**
 * Seeds the alice/bob demo accounts used by the agentic-commerce course demos.
 *
 * <p>Unlike {@link DataSeeder} (dev-only sample data), this runs in every profile:
 * the hosted instance needs these accounts to exist with known credentials, and
 * Bob needs a little confirmed-order history for the "like last time" demo.
 * Self-registration from the demo scripts works but does not survive a database
 * reset; seeding here does.</p>
 *
 * <p>Seeded orders carry idempotency keys prefixed {@code demo-seed-}, which
 * {@code DemoResetService} recognizes and preserves — a demo reset returns the
 * account to this baseline instead of erasing it. Seeding is idempotent: existing
 * users and already-seeded orders are left alone on subsequent startups.</p>
 *
 * <p>The credentials are deliberately public teaching credentials on a mock
 * marketplace (the course repo publishes them, and the demos self-registered
 * these accounts before seeding existed). Kill switch: set
 * {@code DEMO_SEED_ACCOUNTS=false} to disable seeding — including the
 * password-drift revert — without a code change.</p>
 */
@Component
@ConditionalOnProperty(name = "mockhub.demo.seed-accounts", havingValue = "true", matchIfMissing = true)
public class DemoAccountSeeder implements ApplicationRunner, Ordered {

    public static final String DEMO_SEED_KEY_PREFIX = "demo-seed-";

    private static final Logger log = LoggerFactory.getLogger(DemoAccountSeeder.class);
    private static final int BOB_ORDER_COUNT = 3;
    private static final int LISTING_CANDIDATE_POOL = 50;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderService orderService;

    public DemoAccountSeeder(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             ListingRepository listingRepository,
                             OrderRepository orderRepository,
                             CartService cartService,
                             OrderService orderService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @Override
    public int getOrder() {
        return 20; // after DataSeeder(10) so dev-profile listings exist before Bob buys
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureUser("alice@mockhub.com", "alicedemo123", "Alice", "Demo");
            User bob = ensureUser("bob@mockhub.com", "bobdemo123", "Bob", "Demo");
            seedOrderHistory(bob);
        } catch (RuntimeException ex) {
            log.error("Demo account seeding failed: {}", ex.getMessage(), ex);
        }
    }

    private User ensureUser(String email, String password, String firstName, String lastName) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = new User();
            created.setEmail(email);
            created.setFirstName(firstName);
            created.setLastName(lastName);
            created.setPasswordHash(passwordEncoder.encode(password));
            log.info("Seeding demo account {}", email);
            return created;
        });

        // Demo accounts must stay usable with their published credentials: a
        // rehearsal (or a curious student) changing the password would break the
        // next class, so drift is reverted on startup.
        boolean changed = false;
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }
        if (!user.isEnabled() || !user.isEmailVerified()) {
            user.setEnabled(true);
            user.setEmailVerified(true);
            changed = true;
        }
        if (user.getRoles().stream().noneMatch(role -> "ROLE_USER".equals(role.getName()))) {
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
            user.getRoles().add(roleUser);
            changed = true;
        }
        if (changed || user.getId() == null) {
            user = userRepository.save(user);
        }
        return user;
    }

    private void seedOrderHistory(User bob) {
        List<Listing> candidates = listingRepository.findCheapestActiveListingsForFutureEvents(
                Instant.now(), PageRequest.of(0, LISTING_CANDIDATE_POOL));
        Set<Long> usedEventIds = new HashSet<>();

        for (int i = 1; i <= BOB_ORDER_COUNT; i++) {
            String idempotencyKey = DEMO_SEED_KEY_PREFIX + "bob-" + i;
            // Fetch-joined: run() has no transaction, so lazy items would throw on
            // the restart after the first seed
            Optional<Order> existing = orderRepository.findByIdempotencyKeyWithItems(idempotencyKey);
            if (existing.isPresent()) {
                existing.get().getItems().forEach(item ->
                        usedEventIds.add(item.getListing().getEvent().getId()));
                continue;
            }

            Listing listing = candidates.stream()
                    .filter(candidate -> !usedEventIds.contains(candidate.getEvent().getId()))
                    .findFirst()
                    .orElse(null);
            if (listing == null) {
                log.warn("No available listing for demo order {} — skipping", idempotencyKey);
                continue;
            }

            try {
                cartService.clearCart(bob);
                cartService.addToCart(bob, listing.getId());
                OrderDto order = orderService.checkout(bob, new CheckoutRequest("mock"), idempotencyKey);
                orderService.confirmOrder(order.orderNumber());
                usedEventIds.add(listing.getEvent().getId());
                log.info("Seeded demo order {} for {} ({})",
                        order.orderNumber(), bob.getEmail(), listing.getEvent().getName());
            } catch (RuntimeException ex) {
                // A missed order just means less demo history — never block startup
                log.warn("Could not seed demo order {}: {}", idempotencyKey, ex.getMessage());
                usedEventIds.add(listing.getEvent().getId());
            }
        }
    }
}
