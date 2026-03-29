package com.mockhub.auth.service;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.dto.LoginRequest;
import com.mockhub.auth.dto.RegisterRequest;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role buyerRole;

    @BeforeEach
    void setUp() {
        buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded-password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("555-0100");
        testUser.setRoles(Set.of(buyerRole));
        testUser.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("register - given valid request - returns auth response with token")
    void register_givenValidRequest_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com", "password123", "Jane", "Smith", null);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_BUYER")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            user.setCreatedAt(Instant.now());
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("jwt-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response, "Auth response should not be null");
        assertEquals("jwt-token", response.accessToken(), "Access token should match");
        assertEquals("Jane", response.user().firstName(), "First name should match");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - given duplicate email - throws ConflictException")
    void register_givenDuplicateEmail_throwsConflictException() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password123", "Jane", "Smith", null);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(request),
                "Should throw ConflictException for duplicate email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - given valid credentials - returns auth response")
    void login_givenValidCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("jwt-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.login(request);

        assertNotNull(response, "Auth response should not be null");
        assertEquals("jwt-token", response.accessToken(), "Access token should match");
        assertEquals("test@example.com", response.user().email(), "Email should match");
    }

    @Test
    @DisplayName("login - given invalid credentials - throws exception")
    void login_givenInvalidCredentials_throwsException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request),
                "Should throw BadCredentialsException for invalid credentials");
    }

    @Test
    @DisplayName("refreshToken - given valid token - returns new auth response")
    void refreshToken_givenValidToken_returnsNewAuthResponse() {
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-refresh-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.refreshToken("valid-refresh-token");

        assertNotNull(response, "Auth response should not be null");
        assertEquals("new-access-token", response.accessToken(), "Should return new access token");
    }

    @Test
    @DisplayName("refreshToken - given invalid token - throws UnauthorizedException")
    void refreshToken_givenInvalidToken_throwsUnauthorizedException() {
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken("invalid-token"),
                "Should throw UnauthorizedException for invalid refresh token");
    }

    @Test
    @DisplayName("getCurrentUser - given existing email - returns user DTO")
    void getCurrentUser_givenExistingEmail_returnsUserDto() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserDto result = authService.getCurrentUser("test@example.com");

        assertNotNull(result, "User DTO should not be null");
        assertEquals("test@example.com", result.email(), "Email should match");
        assertEquals("John", result.firstName(), "First name should match");
    }

    @Test
    @DisplayName("getCurrentUser - given unknown email - throws ResourceNotFoundException")
    void getCurrentUser_givenUnknownEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUser("unknown@example.com"),
                "Should throw ResourceNotFoundException for unknown email");
    }

    @Test
    @DisplayName("getLinkedProviders - given user with linked accounts - returns provider names")
    void getLinkedProviders_givenUserWithLinkedAccounts_returnsProviderNames() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        OAuthAccount googleAccount = new OAuthAccount();
        googleAccount.setProvider("google");
        googleAccount.setProviderAccountId("google-id-123");

        OAuthAccount githubAccount = new OAuthAccount();
        githubAccount.setProvider("github");
        githubAccount.setProviderAccountId("github-id-456");

        when(oAuthAccountRepository.findByUserId(1L)).thenReturn(List.of(googleAccount, githubAccount));

        List<String> providers = authService.getLinkedProviders("test@example.com");

        assertEquals(2, providers.size(), "Should return 2 providers");
        assertEquals("google", providers.get(0), "First provider should be google");
        assertEquals("github", providers.get(1), "Second provider should be github");
    }

    @Test
    @DisplayName("getLinkedProviders - given user with no linked accounts - returns empty list")
    void getLinkedProviders_givenNoLinkedAccounts_returnsEmptyList() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.findByUserId(1L)).thenReturn(List.of());

        List<String> providers = authService.getLinkedProviders("test@example.com");

        assertEquals(0, providers.size(), "Should return empty list");
    }

    @Test
    @DisplayName("getLinkedProviders - given unknown email - throws ResourceNotFoundException")
    void getLinkedProviders_givenUnknownEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getLinkedProviders("unknown@example.com"),
                "Should throw ResourceNotFoundException for unknown email");
    }

    @Test
    @DisplayName("unlinkProvider - given linked provider and user has password - removes provider")
    void unlinkProvider_givenLinkedProviderAndPassword_removesProvider() {
        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.existsByUserIdAndProvider(1L, "google")).thenReturn(true);
        when(oAuthAccountRepository.countByUserId(1L)).thenReturn(1L);

        authService.unlinkProvider("test@example.com", "google");

        verify(oAuthAccountRepository).deleteByUserIdAndProvider(1L, "google");
    }

    @Test
    @DisplayName("unlinkProvider - given linked provider and multiple providers no password - removes provider")
    void unlinkProvider_givenMultipleProvidersNoPassword_removesProvider() {
        testUser.setPasswordHash(null);
        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.existsByUserIdAndProvider(1L, "google")).thenReturn(true);
        when(oAuthAccountRepository.countByUserId(1L)).thenReturn(2L);

        authService.unlinkProvider("test@example.com", "google");

        verify(oAuthAccountRepository).deleteByUserIdAndProvider(1L, "google");
    }

    @Test
    @DisplayName("unlinkProvider - given only provider and no password - throws ConflictException")
    void unlinkProvider_givenOnlyProviderAndNoPassword_throwsConflictException() {
        testUser.setPasswordHash(null);
        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.existsByUserIdAndProvider(1L, "google")).thenReturn(true);
        when(oAuthAccountRepository.countByUserId(1L)).thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> authService.unlinkProvider("test@example.com", "google"),
                "Should prevent unlinking last login method");
        verify(oAuthAccountRepository, never()).deleteByUserIdAndProvider(any(), any());
    }

    @Test
    @DisplayName("unlinkProvider - given provider not linked - throws ResourceNotFoundException")
    void unlinkProvider_givenProviderNotLinked_throwsResourceNotFoundException() {
        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.existsByUserIdAndProvider(1L, "github")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> authService.unlinkProvider("test@example.com", "github"),
                "Should throw when provider not linked");
    }
}
