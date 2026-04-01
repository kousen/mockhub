package com.mockhub.ticketmaster.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MockTicketmasterServiceTest {

    private final MockTicketmasterService service = new MockTicketmasterService();

    @Test
    void searchEvents_givenMusicClassification_returnsMockEvent() {
        List<TicketmasterEventResponse> events = service.searchEvents(
                "music", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", 20, 0);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().name()).isEqualTo("Mock Concert - Test Artist");
        assertThat(events.getFirst().id()).isEqualTo("MOCK-TM-001");
    }

    @Test
    void searchEvents_givenMusicClassificationCaseSensitive_returnsMockEvent() {
        List<TicketmasterEventResponse> events = service.searchEvents(
                "Music", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", 20, 0);

        assertThat(events).hasSize(1);
    }

    @Test
    void searchEvents_givenNonMusicClassification_returnsEmpty() {
        List<TicketmasterEventResponse> events = service.searchEvents(
                "sports", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", 20, 0);

        assertThat(events).isEmpty();
    }

    @Test
    void searchEvents_givenMockEvent_hasEmbeddedVenueAndAttraction() {
        List<TicketmasterEventResponse> events = service.searchEvents(
                "music", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z", 20, 0);

        TicketmasterEventResponse event = events.getFirst();
        assertThat(event.embedded()).isNotNull();
        assertThat(event.embedded().venues()).hasSize(1);
        assertThat(event.embedded().venues().getFirst().name()).isEqualTo("Mock Arena");
        assertThat(event.embedded().attractions()).hasSize(1);
        assertThat(event.embedded().attractions().getFirst().name()).isEqualTo("Test Artist");
        assertThat(event.embedded().attractions().getFirst().externalLinks()).containsKey("spotify");
    }
}
