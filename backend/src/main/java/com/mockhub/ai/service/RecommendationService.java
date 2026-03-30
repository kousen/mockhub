package com.mockhub.ai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.ai.dto.RecommendationsResponse;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.entity.Favorite;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.spotify.dto.SpotifyListeningDto;
import com.mockhub.spotify.service.SpotifyListeningService;

@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final EventRepository eventRepository;
    private final EvalRunner evalRunner;
    private final FavoriteRepository favoriteRepository;
    private final OrderItemRepository orderItemRepository;
    private final Optional<SpotifyListeningService> spotifyListeningService;

    public RecommendationService(ChatClient chatClient, EventRepository eventRepository,
                                  EvalRunner evalRunner, FavoriteRepository favoriteRepository,
                                  OrderItemRepository orderItemRepository,
                                  Optional<SpotifyListeningService> spotifyListeningService) {
        this.chatClient = chatClient;
        this.eventRepository = eventRepository;
        this.evalRunner = evalRunner;
        this.favoriteRepository = favoriteRepository;
        this.orderItemRepository = orderItemRepository;
        this.spotifyListeningService = spotifyListeningService;
    }

    @Transactional(readOnly = true)
    public RecommendationsResponse getRecommendations(Long userId, String city) {
        SpotifyListeningDto listeningData = fetchListeningData(userId);

        List<Event> events = getCandidateEvents(city, listeningData);
        if (events.isEmpty()) {
            return new RecommendationsResponse(Collections.emptyList(),
                    listeningData.spotifyConnected(), listeningData.scopeUpgradeNeeded());
        }

        // Track which events came from Spotify matching
        Set<Long> spotifyMatchedIds = getSpotifyMatchedEventIds(listeningData);

        String eventList = formatEventList(events);
        String userContext = buildUserContext(userId, listeningData);
        String prompt = buildPrompt(eventList, userContext);

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        List<RecommendationDto> recommendations = parseRecommendations(aiResponse, events, spotifyMatchedIds);

        EvalContext evalContext = EvalContext.forRecommendations(recommendations);
        EvalSummary evalSummary = evalRunner.evaluate(evalContext);
        if (!evalSummary.allPassed()) {
            log.warn("Recommendation eval conditions flagged issues: {}", evalSummary.failures());
        }

        return new RecommendationsResponse(recommendations,
                listeningData.spotifyConnected(), listeningData.scopeUpgradeNeeded());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("java:S6809") // Delegate to sibling @Transactional method; same readOnly semantics
    public List<RecommendationDto> getRecommendations(Long userId) {
        return getRecommendations(userId, null).recommendations();
    }

    private SpotifyListeningDto fetchListeningData(Long userId) {
        if (userId == null || spotifyListeningService.isEmpty()) {
            return SpotifyListeningDto.notConnected();
        }
        try {
            return spotifyListeningService.get().getListeningData(userId);
        } catch (Exception e) {
            log.warn("Failed to fetch Spotify listening data: {}", e.getMessage());
            return SpotifyListeningDto.notConnected();
        }
    }

    private List<Event> getCandidateEvents(String city, SpotifyListeningDto listeningData) {
        // Start with featured events, optionally filtered by city
        List<Event> featured;
        if (city != null && !city.isBlank()) {
            featured = eventRepository.findFeaturedEventsByCity(city);
        } else {
            featured = eventRepository.findFeaturedEvents();
        }

        // Merge in Spotify-matched events (may not be featured)
        List<String> allArtistIds = new ArrayList<>();
        allArtistIds.addAll(listeningData.topArtistIds());
        allArtistIds.addAll(listeningData.recentlyPlayedArtistIds());

        if (allArtistIds.isEmpty()) {
            return featured;
        }

        List<Event> spotifyMatched = eventRepository.findBySpotifyArtistIdIn(allArtistIds);

        // Apply city filter to Spotify-matched events if specified
        if (city != null && !city.isBlank()) {
            String cityLower = city.toLowerCase(java.util.Locale.ROOT);
            spotifyMatched = spotifyMatched.stream()
                    .filter(e -> e.getVenue() != null && e.getVenue().getCity() != null
                            && e.getVenue().getCity().toLowerCase(java.util.Locale.ROOT).equals(cityLower))
                    .toList();
        }

        // Deduplicate by event ID, preserving featured order
        Map<Long, Event> merged = new LinkedHashMap<>();
        for (Event event : featured) {
            merged.put(event.getId(), event);
        }
        for (Event event : spotifyMatched) {
            merged.putIfAbsent(event.getId(), event);
        }

        return new ArrayList<>(merged.values());
    }

    private Set<Long> getSpotifyMatchedEventIds(SpotifyListeningDto listeningData) {
        List<String> allArtistIds = new ArrayList<>();
        allArtistIds.addAll(listeningData.topArtistIds());
        allArtistIds.addAll(listeningData.recentlyPlayedArtistIds());

        if (allArtistIds.isEmpty()) {
            return Set.of();
        }

        return eventRepository.findBySpotifyArtistIdIn(allArtistIds).stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
    }

    private String formatEventList(List<Event> events) {
        return events.stream()
                .map(e -> String.format("id=%d, name=\"%s\", venue=\"%s\", city=\"%s\", category=\"%s\", minPrice=$%s",
                        e.getId(), e.getName(),
                        e.getVenue() != null ? e.getVenue().getName() : "TBD",
                        e.getVenue() != null ? e.getVenue().getCity() : "TBD",
                        e.getCategory() != null ? e.getCategory().getName() : "General",
                        e.getMinPrice()))
                .collect(Collectors.joining("\n"));
    }

    private String buildUserContext(Long userId, SpotifyListeningDto listeningData) {
        Set<String> categories = new LinkedHashSet<>();
        Set<String> artists = new LinkedHashSet<>();
        Set<String> cities = new LinkedHashSet<>();

        if (userId != null) {
            List<Favorite> favorites = favoriteRepository.findByUserIdWithEventDetails(userId);
            for (Favorite favorite : favorites) {
                extractEventSignals(favorite.getEvent(), categories, artists, cities);
            }

            List<Event> purchasedEvents = orderItemRepository.findDistinctPurchasedEventsByUserId(userId);
            for (Event event : purchasedEvents) {
                extractEventSignals(event, categories, artists, cities);
            }
        }

        artists.addAll(listeningData.topArtistNames());

        return formatContextString(categories, artists, cities, listeningData.topGenres());
    }

    private void extractEventSignals(Event event, Set<String> categories,
                                      Set<String> artists, Set<String> cities) {
        if (event.getCategory() != null) {
            categories.add(event.getCategory().getName());
        }
        if (event.getArtistName() != null && !event.getArtistName().isBlank()) {
            artists.add(event.getArtistName());
        }
        if (event.getVenue() != null && event.getVenue().getCity() != null) {
            cities.add(event.getVenue().getCity());
        }
    }

    private String formatContextString(Set<String> categories, Set<String> artists,
                                        Set<String> cities, List<String> spotifyGenres) {
        StringBuilder context = new StringBuilder();
        if (!categories.isEmpty()) {
            context.append("The user's favorite categories are: ").append(String.join(", ", categories)).append(".\n");
        }
        if (!artists.isEmpty()) {
            context.append("Artists they like: ").append(String.join(", ", artists)).append(".\n");
        }
        if (!cities.isEmpty()) {
            context.append("Cities they attend events in: ").append(String.join(", ", cities)).append(".\n");
        }
        if (!spotifyGenres.isEmpty()) {
            context.append("Their top music genres on Spotify: ").append(String.join(", ", spotifyGenres)).append(".\n");
        }
        return context.toString();
    }

    private String buildPrompt(String eventList, String userContext) {
        if (userContext.isEmpty()) {
            return String.format(
                    """
                    Rank these events by general appeal and provide a reason for each.

                    Events:
                    %s

                    Respond with ONLY a JSON array (no markdown, no explanation):
                    [{"eventId": <id>, "relevanceScore": <0.0-1.0>, "reason": "<short reason>"}]

                    Return all events, sorted by relevanceScore descending.
                    """,
                    eventList
            );
        }

        return String.format(
                """
                %sRank these events by relevance to this user's preferences and provide a reason for each.

                Events:
                %s

                Respond with ONLY a JSON array (no markdown, no explanation):
                [{"eventId": <id>, "relevanceScore": <0.0-1.0>, "reason": "<short reason>"}]

                Return all events, sorted by relevanceScore descending.
                """,
                userContext, eventList
        );
    }

    private List<RecommendationDto> parseRecommendations(String aiResponse, List<Event> events,
                                                          Set<Long> spotifyMatchedIds) {
        try {
            List<Map<String, Object>> ranked = MAPPER.readValue(
                    aiResponse.strip(), new TypeReference<>() {});

            Map<Long, Event> eventMap = events.stream()
                    .collect(Collectors.toMap(Event::getId, Function.identity()));

            return ranked.stream()
                    .filter(r -> eventMap.containsKey(((Number) r.get("eventId")).longValue()))
                    .map(r -> {
                        Long eventId = ((Number) r.get("eventId")).longValue();
                        Event event = eventMap.get(eventId);
                        double score = ((Number) r.get("relevanceScore")).doubleValue();
                        String reason = (String) r.get("reason");

                        return new RecommendationDto(
                                event.getId(),
                                event.getName(),
                                event.getSlug(),
                                event.getVenue() != null ? event.getVenue().getName() : null,
                                event.getVenue() != null ? event.getVenue().getCity() : null,
                                event.getEventDate(),
                                event.getMinPrice(),
                                score,
                                reason,
                                spotifyMatchedIds.contains(eventId)
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse AI recommendations response: {}", aiResponse, e);
            return events.stream()
                    .map(event -> new RecommendationDto(
                            event.getId(),
                            event.getName(),
                            event.getSlug(),
                            event.getVenue() != null ? event.getVenue().getName() : null,
                            event.getVenue() != null ? event.getVenue().getCity() : null,
                            event.getEventDate(),
                            event.getMinPrice(),
                            0.5,
                            "Featured event",
                            spotifyMatchedIds.contains(event.getId())
                    ))
                    .toList();
        }
    }
}
