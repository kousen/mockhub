package com.mockhub.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class AuthService {

    private static final String USER_RESOURCE = "User";
    private static final String EMAIL_FIELD = "email";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       OAuthAccountRepository oAuthAccountRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        Role buyerRole = roleRepository.findByName("ROLE_BUYER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_BUYER"));

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRoles(Set.of(buyerRole));
        user.setLastLoginAt(Instant.now());

        User savedUser = userRepository.save(user);

        SecurityUser securityUser = new SecurityUser(savedUser);
        String accessToken = jwtTokenProvider.generateAccessToken(securityUser);

        return new AuthResponse(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                toUserDto(savedUser)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, request.email()));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                toUserDto(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new com.mockhub.common.exception.UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, email));

        SecurityUser securityUser = new SecurityUser(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(securityUser);

        return new AuthResponse(
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                toUserDto(user)
        );
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, email));

        return toUserDto(user);
    }

    @Transactional
    public UserDto updateCurrentUser(String email, String firstName, String lastName, String phone) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, email));

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        User updatedUser = userRepository.save(user);
        return toUserDto(updatedUser);
    }

    public String generateRefreshToken(SecurityUser securityUser) {
        return jwtTokenProvider.generateRefreshToken(securityUser);
    }

    @Transactional(readOnly = true)
    public List<String> getLinkedProviders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, email));
        return oAuthAccountRepository.findByUserId(user.getId()).stream()
                .map(OAuthAccount::getProvider)
                .toList();
    }

    private UserDto toUserDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                roleNames,
                user.getCreatedAt()
        );
    }
}
