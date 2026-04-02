package com.mockhub.ticketmaster.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterVenueResponse;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Component
@Profile("ticketmaster")
public class TicketmasterSyncService {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterSyncService.class);

    private static final List<String> CLASSIFICATIONS_TO_SYNC = List.of(
            "music", "sports", "arts & theatre");

    private final TicketmasterService ticketmasterService;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final TicketmasterEventMapper eventMapper;
    private final TicketmasterTicketGenerator ticketGenerator;
    private final int eventsPerCategory;

    public TicketmasterSyncService(
            TicketmasterService ticketmasterService,
            EventRepository eventRepository,
            VenueRepository venueRepository,
            CategoryRepository categoryRepository,
            TicketmasterEventMapper eventMapper,
            TicketmasterTicketGenerator ticketGenerator,
            @Value("${mockhub.ticketmaster.events-per-category:50}") int eventsPerCategory) {
        this.ticketmasterService = ticketmasterService;
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.categoryRepository = categoryRepository;
        this.eventMapper = eventMapper;
        this.ticketGenerator = ticketGenerator;
        this.eventsPerCategory = eventsPerCategory;
    }

    @Scheduled(cron = "${mockhub.ticketmaster.sync-cron:0 0 4 * * *}")
    public void syncEvents() {
        log.info("Starting Ticketmaster event sync");

        // Ticketmaster requires YYYY-MM-DDTHH:mm:ssZ format (no fractional seconds)
        DateTimeFormatter tmFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);
        String startDateTime = tmFormat.format(Instant.now());
        String endDateTime = tmFormat.format(Instant.now().plus(180, ChronoUnit.DAYS));

        int newEvents = 0;
        int updatedEvents = 0;
        int skippedEvents = 0;

        for (String classification : CLASSIFICATIONS_TO_SYNC) {
            try {
                List<TicketmasterEventResponse> events = ticketmasterService.searchEvents(
                        classification, startDateTime, endDateTime, eventsPerCategory, 0);

                for (TicketmasterEventResponse tmEvent : events) {
                    try {
                        SyncResult result = processEvent(tmEvent);
                        switch (result) {
                            case NEW -> newEvents++;
                            case UPDATED -> updatedEvents++;
                            case SKIPPED -> skippedEvents++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to process Ticketmaster event '{}' ({}): {}",
                                tmEvent.name(), tmEvent.id(), e.getMessage());
                        skippedEvents++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch Ticketmaster events for classification '{}': {}",
                        classification, e.getMessage());
            }
        }

        log.info("Ticketmaster sync complete: {} new, {} updated, {} skipped",
                newEvents, updatedEvents, skippedEvents);
    }

    /**
     * Backfill Spotify artist IDs for events that have a Ticketmaster event ID
     * but are missing a Spotify artist ID. Fetches each event individually from
     * the Ticketmaster Discovery API to get the full attraction data.
     */
    @Transactional
    public int backfillSpotifyIds() {
        List<Event> missingSpotify = eventRepository.findMissingSpotifyWithTicketmasterId();
        log.info("Spotify backfill: found {} events missing Spotify IDs", missingSpotify.size());

        int updated = 0;
        for (Event event : missingSpotify) {
            try {
                TicketmasterEventResponse tmEvent = ticketmasterService.getEvent(
                        event.getTicketmasterEventId());
                if (tmEvent == null) {
                    log.debug("Backfill: no TM response for '{}'", event.getName());
                    continue;
                }
                if (tmEvent.embedded() == null
                        || tmEvent.embedded().attractions() == null
                        || tmEvent.embedded().attractions().isEmpty()) {
                    log.debug("Backfill: no attractions for '{}' (embedded={})",
                            event.getName(), tmEvent.embedded() != null ? "present" : "null");
                    continue;
                }

                TicketmasterAttractionResponse attraction = tmEvent.embedded().attractions().getFirst();
                log.debug("Backfill: '{}' attraction='{}' externalLinks={}",
                        event.getName(), attraction.name(),
                        attraction.externalLinks() != null ? attraction.externalLinks().keySet() : "NULL");
                String spotifyId = eventMapper.extractSpotifyArtistId(attraction);
                if (spotifyId != null) {
                    event.setSpotifyArtistId(spotifyId);
                    eventRepository.save(event);
                    log.info("Backfilled Spotify ID '{}' for event '{}' (artist: '{}')",
                            spotifyId, event.getName(), attraction.name());
                    updated++;
                }
            } catch (Exception e) {
                log.warn("Failed to backfill Spotify for event '{}' (TM ID: {}): {}",
                        event.getName(), event.getTicketmasterEventId(), e.getMessage());
            }
        }

        log.info("Spotify backfill complete: {}/{} events updated", updated, missingSpotify.size());
        return updated;
    }

    @Transactional
    SyncResult processEvent(TicketmasterEventResponse tmEvent) {
        if (tmEvent.id() == null || tmEvent.name() == null) {
            return SyncResult.SKIPPED;
        }

        // Skip events without date info
        if (tmEvent.dates() == null || tmEvent.dates().start() == null
                || tmEvent.dates().start().localDate() == null) {
            return SyncResult.SKIPPED;
        }

        // Skip past events
        Instant eventDate = eventMapper.parseEventDate(tmEvent.dates());
        if (eventDate.isBefore(Instant.now())) {
            return SyncResult.SKIPPED;
        }

        // Check if event already exists
        Optional<Event> existingEvent = eventRepository.findByTicketmasterEventId(tmEvent.id());
        if (existingEvent.isPresent()) {
            return updateExistingEvent(existingEvent.get(), tmEvent);
        }

        // Resolve venue
        Venue venue = resolveVenue(tmEvent);
        if (venue == null) {
            log.warn("Could not resolve venue for event: {}", tmEvent.name());
            return SyncResult.SKIPPED;
        }

        // Resolve category
        String categorySlug = eventMapper.resolveCategorySlug(tmEvent.classifications());
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseGet(() -> categoryRepository.findBySlug("other").orElse(null));
        if (category == null) {
            log.warn("Could not resolve category for event: {}", tmEvent.name());
            return SyncResult.SKIPPED;
        }

        // Map and save event
        Event event = eventMapper.mapToEvent(tmEvent, venue, category);
        event.setTotalTickets(venue.getCapacity());
        event.setAvailableTickets(venue.getCapacity());
        Event savedEvent = eventRepository.save(event);

        // Only generate tickets and listings for active events
        if ("ACTIVE".equals(savedEvent.getStatus())) {
            ticketGenerator.generateForEvent(savedEvent);

            // Update ticket counts to match actual generated capacity
            savedEvent.setTotalTickets(savedEvent.getVenue().getCapacity());
            savedEvent.setAvailableTickets(savedEvent.getVenue().getCapacity());
            eventRepository.save(savedEvent);
        }

        log.debug("Created new event from Ticketmaster: {} ({})", savedEvent.getName(), tmEvent.id());
        return SyncResult.NEW;
    }

    private SyncResult updateExistingEvent(Event existing, TicketmasterEventResponse tmEvent) {
        boolean changed = false;

        // Update status if changed
        String newStatus = mapTmStatus(tmEvent);
        if (!newStatus.equals(existing.getStatus())) {
            existing.setStatus(newStatus);
            changed = true;
        }

        // Update event date if changed
        Instant newDate = eventMapper.parseEventDate(tmEvent.dates());
        if (!newDate.equals(existing.getEventDate())) {
            existing.setEventDate(newDate);
            existing.setDoorsOpenAt(newDate.minusSeconds(3600));
            changed = true;
        }

        // Update image if changed
        String newImage = eventMapper.selectBestImage(tmEvent.images());
        if (newImage != null && !newImage.equals(existing.getPrimaryImageUrl())) {
            existing.setPrimaryImageUrl(newImage);
            changed = true;
        }

        // Backfill Spotify artist ID if missing
        if (existing.getSpotifyArtistId() == null && tmEvent.embedded() != null
                && tmEvent.embedded().attractions() != null
                && !tmEvent.embedded().attractions().isEmpty()) {
            TicketmasterAttractionResponse attraction = tmEvent.embedded().attractions().getFirst();
            log.info("Backfill check for '{}': attraction='{}', externalLinks={}",
                    existing.getName(), attraction.name(),
                    attraction.externalLinks() != null ? attraction.externalLinks().keySet() : "NULL");
            String spotifyId = eventMapper.extractSpotifyArtistId(attraction);
            log.info("Backfill result for '{}': spotifyId={}", existing.getName(), spotifyId);
            if (spotifyId != null) {
                existing.setSpotifyArtistId(spotifyId);
                changed = true;
            }
        }

        // Backfill artist name if missing
        if (existing.getArtistName() == null && tmEvent.embedded() != null
                && tmEvent.embedded().attractions() != null
                && !tmEvent.embedded().attractions().isEmpty()) {
            existing.setArtistName(tmEvent.embedded().attractions().getFirst().name());
            changed = true;
        }

        if (changed) {
            eventRepository.save(existing);
            return SyncResult.UPDATED;
        }
        return SyncResult.SKIPPED;
    }

    Venue resolveVenue(TicketmasterEventResponse tmEvent) {
        if (tmEvent.embedded() == null || tmEvent.embedded().venues() == null
                || tmEvent.embedded().venues().isEmpty()) {
            return null;
        }

        TicketmasterVenueResponse tmVenue = tmEvent.embedded().venues().getFirst();

        // 1. Check by Ticketmaster venue ID (sections eagerly loaded via @EntityGraph)
        Optional<Venue> byTmId = venueRepository.findByTicketmasterVenueId(tmVenue.id());
        if (byTmId.isPresent()) {
            return byTmId.get();
        }

        // 2. Check by name + city match (sections eagerly loaded via @EntityGraph)
        String city = tmVenue.city() != null ? tmVenue.city().name() : null;
        if (city != null) {
            Optional<Venue> byNameCity = venueRepository.findByNameAndCity(tmVenue.name(), city);
            if (byNameCity.isPresent()) {
                Venue matched = byNameCity.get();
                matched.setTicketmasterVenueId(tmVenue.id());
                venueRepository.save(matched);
                log.info("Linked existing venue '{}' to Ticketmaster ID: {}",
                        matched.getName(), tmVenue.id());
                return matched;
            }
        }

        // 3. Create new venue
        Venue newVenue = eventMapper.mapToVenue(tmVenue);
        Venue savedVenue = venueRepository.save(newVenue);
        log.info("Created new venue from Ticketmaster: {} ({})", savedVenue.getName(), tmVenue.id());
        return savedVenue;
    }

    private String mapTmStatus(TicketmasterEventResponse tmEvent) {
        if (tmEvent.dates() == null || tmEvent.dates().status() == null
                || tmEvent.dates().status().code() == null) {
            return "ACTIVE";
        }
        return switch (tmEvent.dates().status().code()) {
            case "cancelled" -> "CANCELLED";
            case "postponed" -> "POSTPONED";
            default -> "ACTIVE";
        };
    }

    enum SyncResult {
        NEW, UPDATED, SKIPPED
    }
}
