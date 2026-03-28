package com.mockhub.auth.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.auth.service.AuthService;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private com.mockhub.auth.security.OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    private AuthResponse createTestAuthResponse() {
        UserDto userDto = new UserDto(
                1L, "test@example.com", "John", "Doe",
                null, null, false, Set.of("ROLE_BUYER"), Instant.now());
        return new AuthResponse("jwt-token", 3600L, userDto);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - given valid request - returns 201 with auth response")
    void register_givenValidRequest_returns201WithAuthResponse() throws Exception {
        AuthResponse response = createTestAuthResponse();
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "firstName": "John",
                                    "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - given invalid email - returns 400")
    void register_givenInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "password123",
                                    "firstName": "John",
                                    "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - given missing required fields - returns 400")
    void register_givenMissingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - given duplicate email - returns 409")
    void register_givenDuplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(
                new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "existing@example.com",
                                    "password": "password123",
                                    "firstName": "John",
                                    "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - given valid credentials - returns 200 with auth response")
    void login_givenValidCredentials_returns200WithAuthResponse() throws Exception {
        AuthResponse response = createTestAuthResponse();
        when(authService.login(any())).thenReturn(response);

        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRoles(Set.of(buyerRole));
        SecurityUser securityUser = new SecurityUser(testUser);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        securityUser, null,
                        List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authService.generateRefreshToken(any())).thenReturn("refresh-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - given missing password - returns 400")
    void login_givenMissingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/oauth2/exchange - given valid code - returns 200 with auth response")
    void exchangeOAuth2Code_givenValidCode_returns200WithAuthResponse() throws Exception {
        AuthResponse response = createTestAuthResponse();
        when(oauth2SuccessHandler.exchangeCode("valid-code")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .param("code", "valid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/oauth2/exchange - given invalid code - returns 401")
    void exchangeOAuth2Code_givenInvalidCode_returns401() throws Exception {
        when(oauth2SuccessHandler.exchangeCode("invalid-code")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .param("code", "invalid-code"))
                .andExpect(status().isUnauthorized());
    }
}
