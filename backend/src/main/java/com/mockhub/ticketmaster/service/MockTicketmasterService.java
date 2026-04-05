package com.mockhub.ticketmaster.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse.ExternalLink;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Embedded;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Genre;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Image;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.PriceRange;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Segment;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Start;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Status;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.SubGenre;
import com.mockhub.ticketmaster.dto.TicketmasterVenueResponse;

@Service
@Profile("mock-ticketmaster")
public class MockTicketmasterService implements TicketmasterService {

    private static final Logger log = LoggerFactory.getLogger(MockTicketmasterService.class);

    @Override
    public TicketmasterEventResponse getEvent(String eventId) {
        log.info("Mock Ticketmaster: getEvent not supported, returning null for '{}'", eventId);
        return null;
    }

    @Override
    public List<TicketmasterEventResponse> searchEvents(String classificationName,
                                                         String startDateTime,
                                                         String endDateTime,
                                                         int size,
                                                         int page) {
        log.info("Mock Ticketmaster: returning hardcoded events for '{}'", classificationName);

        if ("music".equalsIgnoreCase(classificationName) || "Music".equals(classificationName)) {
            return List.of(createMockMusicEvent());
        }
        return List.of();
    }

    private TicketmasterEventResponse createMockMusicEvent() {
        return TicketmasterEventResponse.builder()
                .id("MOCK-TM-001")
                .name("Mock Concert - Test Artist")
                .url("https://www.ticketmaster.com/mock")
                .dates(new Dates(
                        new Start("2026-08-15", "20:00:00", "2026-08-16T01:00:00Z", false, false),
                        "America/New_York",
                        new Status("onsale")))
                .classifications(List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Alternative"))))
                .images(List.of(new Image("https://example.com/mock-image.jpg", "16_9", 1024, 576, false)))
                .priceRanges(List.of(new PriceRange("standard", "USD", 45.0, 125.0)))
                .embedded(new Embedded(
                        List.of(new TicketmasterVenueResponse(
                                "MOCK-VENUE-001", "Mock Arena",
                                new TicketmasterVenueResponse.Address("123 Test St"),
                                new TicketmasterVenueResponse.City("New York"),
                                new TicketmasterVenueResponse.State("New York", "NY"),
                                "10001",
                                new TicketmasterVenueResponse.Country("United States", "US"),
                                new TicketmasterVenueResponse.Location("40.750580", "-73.993580"))),
                        List.of(new TicketmasterAttractionResponse(
                                "MOCK-ATTR-001", "Test Artist",
                                Map.of("spotify", List.of(
                                        new ExternalLink("https://open.spotify.com/artist/4Z8W4fKeB5YxbusRsdQVPb", null)))))))
                .build();
    }
}
