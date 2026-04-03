package com.mockhub.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@example.com");
        testUser.setFirstName("Admin");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(buyerRole));
        testUser.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("getAllUsers - given users exist - returns paged user DTOs")
    void getAllUsers_givenUsersExist_returnsPagedUserDtos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<UserDto> result = adminUserService.getAllUsers(pageable);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one user");
        assertEquals("admin@example.com", result.content().get(0).email(), "Email should match");
    }

    @Test
    @DisplayName("getUserCount - returns total user count")
    void getUserCount_returnsTotalUserCount() {
        when(userRepository.count()).thenReturn(100L);

        long result = adminUserService.getUserCount();

        assertEquals(100L, result);
    }

    @Test
    @DisplayName("updateUserRoles - given valid roles - updates user roles")
    void updateUserRoles_givenValidRoles_updatesUserRoles() {
        Role adminRole = new Role("ROLE_ADMIN");
        adminRole.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        adminUserService.updateUserRoles(1L, Set.of("ROLE_ADMIN"));

        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserRoles - given nonexistent user - throws ResourceNotFoundException")
    void updateUserRoles_givenNonexistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.updateUserRoles(999L, Set.of("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("updateUserStatus - given existing user - updates enabled flag")
    void updateUserStatus_givenExistingUser_updatesEnabledFlag() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        adminUserService.updateUserStatus(1L, false);

        assertFalse(testUser.isEnabled(), "User should be disabled");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserStatus - given nonexistent user - throws ResourceNotFoundException")
    void updateUserStatus_givenNonexistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.updateUserStatus(999L, true));
    }
}
