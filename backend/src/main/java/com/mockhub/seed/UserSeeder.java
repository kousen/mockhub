package com.mockhub.seed;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;

import net.datafaker.Faker;

@Component
public class UserSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker();

    public UserSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void seed() {
        if (userRepository.existsByEmail("admin@mockhub.com")) {
            log.info("Users already seeded, skipping");
            return;
        }

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role("ROLE_USER");
                    return roleRepository.save(role);
                });

        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role("ROLE_ADMIN");
                    return roleRepository.save(role);
                });

        // Admin user
        createUser("admin@mockhub.com", "admin123", "Admin", "User",
                "555-0100", Set.of(roleAdmin, roleUser));

        // Buyer user
        createUser("buyer@mockhub.com", "buyer123", "Jane", "Buyer",
                "555-0101", Set.of(roleUser));

        // Seller user
        createUser("seller@mockhub.com", "seller123", "John", "Seller",
                "555-0102", Set.of(roleUser));

        // 5 additional random users
        for (int i = 0; i < 5; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
            String phone = faker.phoneNumber().subscriberNumber(10);
            createUser(email, "password123", firstName, lastName, phone, Set.of(roleUser));
        }

        log.info("Seeded 8 users (admin, buyer, seller, 5 random)");
    }

    private void createUser(String email, String password, String firstName,
                            String lastName, String phone, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }
}
