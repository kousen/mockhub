package com.mockhub.favorite.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.favorite.service.FavoriteService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/v1/favorites - unauthenticated - returns 403")
    void listFavorites_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{eventId} - unauthenticated - returns 403")
    void addFavorite_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{eventId} - unauthenticated - returns 403")
    void removeFavorite_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/favorites/check/{eventId} - unauthenticated - returns 403")
    void checkFavorite_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/favorites/check/1"))
                .andExpect(status().isForbidden());
    }
}
