package com.mockhub.favorite.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.favorite.service.FavoriteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "Favorites", description = "Manage favorited events")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService,
                              UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List favorites", description = "Return all events favorited by the current user")
    @ApiResponse(responseCode = "200", description = "Favorites returned")
    public ResponseEntity<List<EventSummaryDto>> listFavorites(
            @AuthenticationPrincipal SecurityUser securityUser) {
        User user = resolveUser(securityUser);
        return ResponseEntity.ok(favoriteService.getUserFavorites(user));
    }

    @PostMapping("/{eventId}")
    @Operation(summary = "Add favorite", description = "Add an event to the user's favorites")
    @ApiResponse(responseCode = "201", description = "Favorite added")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long eventId) {
        User user = resolveUser(securityUser);
        favoriteService.addFavorite(user, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Remove favorite", description = "Remove an event from the user's favorites")
    @ApiResponse(responseCode = "204", description = "Favorite removed")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long eventId) {
        User user = resolveUser(securityUser);
        favoriteService.removeFavorite(user, eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{eventId}")
    @Operation(summary = "Check favorite status", description = "Check if an event is in the user's favorites")
    @ApiResponse(responseCode = "200", description = "Favorite status returned")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long eventId) {
        User user = resolveUser(securityUser);
        boolean favorited = favoriteService.isFavorited(user, eventId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    private User resolveUser(SecurityUser securityUser) {
        return userRepository.findByEmail(securityUser.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", securityUser.getEmail()));
    }
}
