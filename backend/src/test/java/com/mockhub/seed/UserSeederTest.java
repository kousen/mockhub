package com.mockhub.seed;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSeeder userSeeder;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    @DisplayName("seed - given existing users with modified phone - resets to default")
    void seed_givenExistingUsersWithModifiedPhone_resetsToDefault() {
        when(userRepository.existsByEmail("admin@mockhub.com")).thenReturn(true);

        User buyer = new User();
        buyer.setEmail("buyer@mockhub.com");
        buyer.setPhone("+18605551234"); // modified from default
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(buyer));

        User admin = new User();
        admin.setEmail("admin@mockhub.com");
        admin.setPhone("555-0100"); // unchanged
        when(userRepository.findByEmail("admin@mockhub.com")).thenReturn(Optional.of(admin));

        User seller = new User();
        seller.setEmail("seller@mockhub.com");
        seller.setPhone("555-0102"); // unchanged
        when(userRepository.findByEmail("seller@mockhub.com")).thenReturn(Optional.of(seller));

        userSeeder.seed();

        verify(userRepository).save(buyer);
        assertEquals("555-0101", buyer.getPhone());
    }

    @Test
    @DisplayName("seed - given existing users with default phone - does not save")
    void seed_givenExistingUsersWithDefaultPhone_doesNotSave() {
        when(userRepository.existsByEmail("admin@mockhub.com")).thenReturn(true);

        User buyer = new User();
        buyer.setEmail("buyer@mockhub.com");
        buyer.setPhone("555-0101"); // already default
        when(userRepository.findByEmail("buyer@mockhub.com")).thenReturn(Optional.of(buyer));

        User admin = new User();
        admin.setEmail("admin@mockhub.com");
        admin.setPhone("555-0100");
        when(userRepository.findByEmail("admin@mockhub.com")).thenReturn(Optional.of(admin));

        User seller = new User();
        seller.setEmail("seller@mockhub.com");
        seller.setPhone("555-0102");
        when(userRepository.findByEmail("seller@mockhub.com")).thenReturn(Optional.of(seller));

        userSeeder.seed();

        verify(userRepository, never()).save(any(User.class));
    }
}
