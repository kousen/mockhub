package com.mockhub.favorite.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.entity.Favorite;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role("ROLE_BUYER");
        buyerRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRoles(Set.of(buyerRole));

        Venue testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Test Venue");
        testVenue.setCity("New York");

        Category testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Concert");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");
        testEvent.setArtistName("Test Artist");
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        testEvent.setMinPrice(new BigDecimal("50.00"));
        testEvent.setAvailableTickets(100);
        testEvent.setFeatured(false);
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setTags(new HashSet<>());
    }

    @Test
    @DisplayName("addFavorite - given new favorite - saves it")
    void addFavorite_givenNewFavorite_savesIt() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(favoriteRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);

        favoriteService.addFavorite(testUser, 1L);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite - given already favorited - does not create duplicate")
    void addFavorite_givenAlreadyFavorited_doesNotCreateDuplicate() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(favoriteRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        favoriteService.addFavorite(testUser, 1L);

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite - given nonexistent event - throws ResourceNotFoundException")
    void addFavorite_givenNonexistentEvent_throwsResourceNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> favoriteService.addFavorite(testUser, 999L),
                "Should throw ResourceNotFoundException for unknown event");
    }

    @Test
    @DisplayName("removeFavorite - given existing favorite - removes it")
    void removeFavorite_givenExistingFavorite_removesIt() {
        when(favoriteRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        favoriteService.removeFavorite(testUser, 1L);

        verify(favoriteRepository).deleteByUserIdAndEventId(1L, 1L);
    }

    @Test
    @DisplayName("removeFavorite - given nonexistent favorite - throws ResourceNotFoundException")
    void removeFavorite_givenNonexistentFavorite_throwsResourceNotFoundException() {
        when(favoriteRepository.existsByUserIdAndEventId(1L, 999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> favoriteService.removeFavorite(testUser, 999L),
                "Should throw ResourceNotFoundException for unfavorited event");
    }

    @Test
    @DisplayName("isFavorited - given favorited event - returns true")
    void isFavorited_givenFavoritedEvent_returnsTrue() {
        when(favoriteRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        boolean result = favoriteService.isFavorited(testUser, 1L);

        assertTrue(result, "Should return true for favorited event");
    }

    @Test
    @DisplayName("isFavorited - given non-favorited event - returns false")
    void isFavorited_givenNonFavoritedEvent_returnsFalse() {
        when(favoriteRepository.existsByUserIdAndEventId(1L, 999L)).thenReturn(false);

        boolean result = favoriteService.isFavorited(testUser, 999L);

        assertFalse(result, "Should return false for non-favorited event");
    }

    @Test
    @DisplayName("getUserFavorites - given user with favorites - returns event summaries")
    void getUserFavorites_givenUserWithFavorites_returnsEventSummaries() {
        Favorite favorite = new Favorite();
        favorite.setId(1L);
        favorite.setUser(testUser);
        favorite.setEvent(testEvent);

        when(favoriteRepository.findByUserId(1L)).thenReturn(List.of(favorite));

        List<EventSummaryDto> result = favoriteService.getUserFavorites(testUser);

        assertEquals(1, result.size(), "Should return one favorite");
        assertEquals("Test Event", result.get(0).name(), "Event name should match");
    }
}
