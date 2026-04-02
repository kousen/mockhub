package com.mockhub.ticketmaster.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse.ExternalLink;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Image;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.PriceRange;
import com.mockhub.ticketmaster.dto.TicketmasterVenueResponse;
import com.mockhub.venue.entity.Venue;

@Component
public class TicketmasterEventMapper {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterEventMapper.class);

    private static final BigDecimal DEFAULT_BASE_PRICE = new BigDecimal("50.00");
    private static final BigDecimal MIN_PRICE_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal MAX_PRICE_FACTOR = new BigDecimal("2.50");
    private static final int DEFAULT_VENUE_CAPACITY = 1000;

    // Standard artist URL: /artist/{22-char-id} with boundary to prevent overlong matches
    private static final Pattern ARTIST_URL_PATTERN =
            Pattern.compile("open\\.spotify\\.com/artist/([a-zA-Z0-9]{22})(?:[?/]|$)");

    // Mangled URI in user path: /user/spotify:artist:{22-char-id}
    private static final Pattern MANGLED_ARTIST_PATTERN =
            Pattern.compile("open\\.spotify\\.com/user/spotify:artist:([a-zA-Z0-9]{22})(?:[?/]|$)");

    public Event mapToEvent(TicketmasterEventResponse response, Venue venue, Category category) {
        Event event = new Event();
        event.setName(response.name());
        event.setSlug(generateSlug(response.name(), response.id()));
        event.setTicketmasterEventId(response.id());
        event.setVenue(venue);
        event.setCategory(category);
        event.setStatus(mapStatus(response.dates()));
        event.setFeatured(true);

        Instant eventDate = parseEventDate(response.dates());
        event.setEventDate(eventDate);
        Instant doorsOpen = parseDoorsTimes(response.doorsTimes(), response.dates());
        event.setDoorsOpenAt(doorsOpen != null ? doorsOpen : eventDate.minusSeconds(3600));

        BigDecimal basePrice = extractBasePrice(response.priceRanges());
        event.setBasePrice(basePrice);
        event.setMinPrice(basePrice.multiply(MIN_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP));
        event.setMaxPrice(basePrice.multiply(MAX_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP));

        event.setTotalTickets(venue.getCapacity());
        event.setAvailableTickets(venue.getCapacity());

        event.setPrimaryImageUrl(selectBestImage(response.images()));

        String spotifyId = extractSpotifyIdFromEmbedded(response);
        event.setSpotifyArtistId(spotifyId);

        String artistName = extractArtistName(response);
        event.setArtistName(artistName);

        return event;
    }

    public Venue mapToVenue(TicketmasterVenueResponse response) {
        Venue venue = new Venue();
        String venueName = response.name() != null ? response.name() : "Unknown Venue";
        venue.setName(venueName);
        venue.setSlug(generateSlug(venueName, response.id()));
        venue.setTicketmasterVenueId(response.id());
        venue.setCapacity(DEFAULT_VENUE_CAPACITY);
        venue.setVenueType("ARENA");

        venue.setAddressLine1(response.address() != null && response.address().line1() != null
                ? response.address().line1() : "Unknown");
        venue.setCity(response.city() != null && response.city().name() != null
                ? response.city().name() : "Unknown");
        venue.setState(response.state() != null && response.state().stateCode() != null
                ? response.state().stateCode() : "N/A");
        venue.setZipCode(response.postalCode() != null ? response.postalCode() : "00000");
        venue.setCountry(response.country() != null && response.country().countryCode() != null
                ? response.country().countryCode() : "US");

        if (response.location() != null) {
            if (response.location().latitude() != null) {
                venue.setLatitude(new BigDecimal(response.location().latitude())
                        .setScale(6, RoundingMode.HALF_UP));
            }
            if (response.location().longitude() != null) {
                venue.setLongitude(new BigDecimal(response.location().longitude())
                        .setScale(6, RoundingMode.HALF_UP));
            }
        }

        return venue;
    }

    public String resolveCategorySlug(List<Classification> classifications) {
        if (classifications == null || classifications.isEmpty()) {
            return "other";
        }

        Classification primary = classifications.stream()
                .filter(c -> Boolean.TRUE.equals(c.primary()))
                .findFirst()
                .orElse(classifications.getFirst());

        // Check genre first for more specific matches
        if (primary.genre() != null && "Comedy".equalsIgnoreCase(primary.genre().name())) {
            return "comedy";
        }

        if (primary.segment() == null || primary.segment().name() == null) {
            return "other";
        }

        return switch (primary.segment().name()) {
            case "Music" -> "concerts";
            case "Sports" -> "sports";
            case "Arts & Theatre" -> "theater";
            default -> "other";
        };
    }

    public String extractSpotifyArtistId(TicketmasterAttractionResponse attraction) {
        if (attraction == null || attraction.externalLinks() == null) {
            return null;
        }

        List<ExternalLink> spotifyLinks = attraction.externalLinks().get("spotify");
        if (spotifyLinks == null || spotifyLinks.isEmpty()) {
            return null;
        }

        for (ExternalLink link : spotifyLinks) {
            String url = link.url();
            if (url == null) {
                continue;
            }
            String artistId = extractArtistIdFromUrl(url);
            if (artistId != null) {
                return artistId;
            }
        }
        return null;
    }

    /**
     * Extract a Spotify artist ID from a URL. Handles two formats found in
     * Ticketmaster production data:
     * <ul>
     *   <li>Standard: {@code open.spotify.com/artist/{22-char-id}}</li>
     *   <li>Mangled:  {@code open.spotify.com/user/spotify:artist:{22-char-id}}</li>
     * </ul>
     * Returns null for playlists, user profiles, non-Spotify URLs, and other
     * formats that don't contain a recoverable artist ID.
     */
    String extractArtistIdFromUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher artistMatcher = ARTIST_URL_PATTERN.matcher(url);
        if (artistMatcher.find()) {
            return artistMatcher.group(1);
        }

        Matcher mangledMatcher = MANGLED_ARTIST_PATTERN.matcher(url);
        if (mangledMatcher.find()) {
            return mangledMatcher.group(1);
        }

        return null;
    }

    public String selectBestImage(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        // Prefer 16:9 images, then pick the largest by width
        Comparator<Image> byWidth = Comparator.comparingInt(
                img -> img.width() != null ? img.width() : 0);
        return images.stream()
                .filter(img -> "16_9".equals(img.ratio()))
                .max(byWidth)
                .map(Image::url)
                .orElseGet(() -> images.stream()
                        .max(byWidth)
                        .map(Image::url)
                        .orElse(null));
    }

    public BigDecimal extractBasePrice(List<PriceRange> priceRanges) {
        if (priceRanges == null || priceRanges.isEmpty()) {
            return DEFAULT_BASE_PRICE;
        }

        Double min = priceRanges.getFirst().min();
        if (min == null) {
            return DEFAULT_BASE_PRICE;
        }
        return BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP);
    }

    public Instant parseEventDate(Dates dates) {
        if (dates == null || dates.start() == null) {
            return Instant.now();
        }

        // Prefer UTC dateTime if available
        if (dates.start().dateTime() != null) {
            return Instant.parse(dates.start().dateTime());
        }

        // Fall back to localDate + localTime + timezone
        LocalDate localDate = LocalDate.parse(dates.start().localDate());
        LocalTime localTime = dates.start().localTime() != null
                ? LocalTime.parse(dates.start().localTime())
                : LocalTime.of(20, 0);
        ZoneId zone = dates.timezone() != null
                ? ZoneId.of(dates.timezone())
                : ZoneId.of("America/New_York");

        return ZonedDateTime.of(localDate, localTime, zone).toInstant();
    }

    public Instant parseDoorsTimes(TicketmasterEventResponse.DoorsTimes doorsTimes, Dates dates) {
        if (doorsTimes == null) {
            return null;
        }

        try {
            // Prefer UTC dateTime if available
            if (doorsTimes.dateTime() != null) {
                return Instant.parse(doorsTimes.dateTime());
            }

            // Fall back to localDate + localTime + event timezone
            if (doorsTimes.localDate() != null && doorsTimes.localTime() != null) {
                LocalDate localDate = LocalDate.parse(doorsTimes.localDate());
                LocalTime localTime = LocalTime.parse(doorsTimes.localTime());
                ZoneId zone = dates != null && dates.timezone() != null
                        ? ZoneId.of(dates.timezone())
                        : ZoneId.of("America/New_York");
                return ZonedDateTime.of(localDate, localTime, zone).toInstant();
            }
        } catch (Exception e) {
            log.warn("Failed to parse doorsTimes ({}), falling back to default: {}",
                    doorsTimes, e.getMessage());
        }

        return null;
    }

    private String mapStatus(Dates dates) {
        if (dates == null || dates.status() == null || dates.status().code() == null) {
            return "ACTIVE";
        }
        return switch (dates.status().code()) {
            case "cancelled" -> "CANCELLED";
            case "postponed" -> "POSTPONED";
            default -> "ACTIVE";
        };
    }

    private String extractSpotifyIdFromEmbedded(TicketmasterEventResponse response) {
        if (response.embedded() == null || response.embedded().attractions() == null
                || response.embedded().attractions().isEmpty()) {
            log.debug("No attractions for event: {}", response.name());
            return null;
        }
        TicketmasterAttractionResponse attraction = response.embedded().attractions().getFirst();
        String spotifyId = extractSpotifyArtistId(attraction);
        if (spotifyId != null) {
            log.debug("Extracted Spotify ID '{}' for attraction '{}' on event '{}'",
                    spotifyId, attraction.name(), response.name());
        } else if (attraction.externalLinks() != null
                && attraction.externalLinks().containsKey("spotify")) {
            List<ExternalLink> links = attraction.externalLinks().get("spotify");
            log.warn("Spotify key exists but no artist ID extracted for '{}'. URLs: {}",
                    attraction.name(),
                    links != null ? links.stream().map(ExternalLink::url).toList() : "null");
        }
        return spotifyId;
    }

    private String extractArtistName(TicketmasterEventResponse response) {
        if (response.embedded() == null || response.embedded().attractions() == null
                || response.embedded().attractions().isEmpty()) {
            return null;
        }
        return response.embedded().attractions().getFirst().name();
    }

    private String generateSlug(String name, String externalId) {
        if (name == null) {
            name = "unknown";
        }
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");

        // Append a short hash of the external ID for uniqueness
        int hash = Math.abs(externalId.hashCode() % 10000);
        return base + "-tm-" + hash;
    }
}
