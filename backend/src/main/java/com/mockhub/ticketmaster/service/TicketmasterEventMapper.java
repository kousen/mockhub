package com.mockhub.ticketmaster.service;

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

import org.springframework.stereotype.Component;

import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Image;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.PriceRange;
import com.mockhub.ticketmaster.dto.TicketmasterVenueResponse;
import com.mockhub.venue.entity.Venue;

@Component
public class TicketmasterEventMapper {

    private static final BigDecimal DEFAULT_BASE_PRICE = new BigDecimal("50.00");
    private static final BigDecimal MIN_PRICE_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal MAX_PRICE_FACTOR = new BigDecimal("2.50");
    private static final int DEFAULT_VENUE_CAPACITY = 1000;

    public Event mapToEvent(TicketmasterEventResponse response, Venue venue, Category category) {
        Event event = new Event();
        event.setName(response.name());
        event.setSlug(generateSlug(response.name(), response.id()));
        event.setTicketmasterEventId(response.id());
        event.setVenue(venue);
        event.setCategory(category);
        event.setStatus(mapStatus(response.dates()));
        event.setFeatured(false);

        Instant eventDate = parseEventDate(response.dates());
        event.setEventDate(eventDate);
        event.setDoorsOpenAt(eventDate.minusSeconds(3600));

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
        venue.setName(response.name());
        venue.setSlug(generateSlug(response.name(), response.id()));
        venue.setTicketmasterVenueId(response.id());
        venue.setCapacity(DEFAULT_VENUE_CAPACITY);
        venue.setVenueType("ARENA");

        venue.setAddressLine1(response.address() != null ? response.address().line1() : "");
        venue.setCity(response.city() != null ? response.city().name() : "");
        venue.setState(response.state() != null ? response.state().stateCode() : "");
        venue.setZipCode(response.postalCode() != null ? response.postalCode() : "");
        venue.setCountry(response.country() != null ? response.country().countryCode() : "US");

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
                .filter(Classification::primary)
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

        List<TicketmasterAttractionResponse.ExternalLink> spotifyLinks =
                attraction.externalLinks().get("spotify");
        if (spotifyLinks == null || spotifyLinks.isEmpty()) {
            return null;
        }

        String url = spotifyLinks.getFirst().url();
        if (url == null || !url.contains("/artist/")) {
            return null;
        }

        // Extract artist ID from URL: https://open.spotify.com/artist/{id}
        String artistId = url.substring(url.lastIndexOf("/artist/") + "/artist/".length());
        // Strip query params if present
        int queryIndex = artistId.indexOf('?');
        if (queryIndex > 0) {
            artistId = artistId.substring(0, queryIndex);
        }
        return artistId.isEmpty() ? null : artistId;
    }

    public String selectBestImage(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        // Prefer 16:9 images, then pick the largest by width
        return images.stream()
                .filter(img -> "16_9".equals(img.ratio()))
                .max(Comparator.comparingInt(Image::width))
                .map(Image::url)
                .orElseGet(() -> images.stream()
                        .max(Comparator.comparingInt(Image::width))
                        .map(Image::url)
                        .orElse(null));
    }

    public BigDecimal extractBasePrice(List<PriceRange> priceRanges) {
        if (priceRanges == null || priceRanges.isEmpty()) {
            return DEFAULT_BASE_PRICE;
        }

        return BigDecimal.valueOf(priceRanges.getFirst().min())
                .setScale(2, RoundingMode.HALF_UP);
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
            return null;
        }
        return extractSpotifyArtistId(response.embedded().attractions().getFirst());
    }

    private String extractArtistName(TicketmasterEventResponse response) {
        if (response.embedded() == null || response.embedded().attractions() == null
                || response.embedded().attractions().isEmpty()) {
            return null;
        }
        return response.embedded().attractions().getFirst().name();
    }

    private String generateSlug(String name, String externalId) {
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
