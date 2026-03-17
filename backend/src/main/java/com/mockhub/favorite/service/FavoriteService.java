package com.mockhub.favorite.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.entity.Favorite;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.venue.entity.Venue;

@Service
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final EventRepository eventRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           EventRepository eventRepository) {
        this.favoriteRepository = favoriteRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void addFavorite(User user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (favoriteRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            log.debug("Event {} already favorited by user {}", eventId, user.getId());
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setEvent(event);
        favoriteRepository.save(favorite);
        log.info("User {} favorited event {}", user.getId(), eventId);
    }

    @Transactional
    public void removeFavorite(User user, Long eventId) {
        if (!favoriteRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new ResourceNotFoundException("Favorite", "eventId", eventId);
        }

        favoriteRepository.deleteByUserIdAndEventId(user.getId(), eventId);
        log.info("User {} unfavorited event {}", user.getId(), eventId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(User user, Long eventId) {
        return favoriteRepository.existsByUserIdAndEventId(user.getId(), eventId);
    }

    @Transactional(readOnly = true)
    public List<EventSummaryDto> getUserFavorites(User user) {
        List<Favorite> favorites = favoriteRepository.findByUserId(user.getId());
        return favorites.stream()
                .map(favorite -> toEventSummaryDto(favorite.getEvent()))
                .toList();
    }

    private EventSummaryDto toEventSummaryDto(Event event) {
        Venue venue = event.getVenue();
        return new EventSummaryDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getArtistName(),
                venue.getName(),
                venue.getCity(),
                event.getEventDate(),
                event.getMinPrice(),
                event.getAvailableTickets(),
                null,
                event.getCategory().getName(),
                event.isFeatured()
        );
    }
}
