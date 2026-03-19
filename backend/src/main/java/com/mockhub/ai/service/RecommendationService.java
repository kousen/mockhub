package com.mockhub.ai.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;

@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final EventRepository eventRepository;

    public RecommendationService(ChatClient chatClient, EventRepository eventRepository) {
        this.chatClient = chatClient;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations() {
        List<Event> events = eventRepository.findFeaturedEvents();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        String eventList = events.stream()
                .map(e -> String.format("id=%d, name=\"%s\", venue=\"%s\", city=\"%s\", category=\"%s\", minPrice=$%s",
                        e.getId(), e.getName(),
                        e.getVenue() != null ? e.getVenue().getName() : "TBD",
                        e.getVenue() != null ? e.getVenue().getCity() : "TBD",
                        e.getCategory() != null ? e.getCategory().getName() : "General",
                        e.getMinPrice()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format(
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

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseRecommendations(aiResponse, events);
    }

    private List<RecommendationDto> parseRecommendations(String aiResponse, List<Event> events) {
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
                                reason
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
                            "Featured event"
                    ))
                    .toList();
        }
    }
}
