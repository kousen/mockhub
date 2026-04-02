package com.mockhub.ticketmaster.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterEventDeserializationTest {

    @Test
    void deserialize_fullEventWithSpotifyAttraction_extractsSpotifyUrl() throws Exception {
        String json = """
                {
                    "id": "1A9ZkoaGkePdD04",
                    "name": "Eagles Live at Sphere",
                    "dates": {
                        "start": {"localDate": "2026-04-10", "dateTime": "2026-04-11T03:30:00Z", "dateTBD": false, "timeTBA": false},
                        "timezone": "America/Los_Angeles",
                        "status": {"code": "onsale"}
                    },
                    "classifications": [{"primary": true, "segment": {"id": "1", "name": "Music"}, "genre": {"id": "1", "name": "Rock"}}],
                    "images": [],
                    "_embedded": {
                        "venues": [{"id": "V1", "name": "Sphere", "city": {"name": "Las Vegas"}, "state": {"stateCode": "NV"}}],
                        "attractions": [
                            {
                                "id": "K8vZ9171ob7",
                                "name": "Eagles",
                                "externalLinks": {
                                    "spotify": [{"url": "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL"}],
                                    "youtube": [{"url": "https://youtube.com/test"}]
                                }
                            }
                        ]
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterEventResponse event = mapper.readValue(json, TicketmasterEventResponse.class);

        assertThat(event.embedded()).isNotNull();
        assertThat(event.embedded().attractions()).hasSize(1);

        TicketmasterAttractionResponse attraction = event.embedded().attractions().getFirst();
        assertThat(attraction.name()).isEqualTo("Eagles");
        assertThat(attraction.externalLinks()).isNotNull();
        assertThat(attraction.externalLinks()).containsKey("spotify");
        assertThat(attraction.externalLinks().get("spotify").getFirst().url())
                .isEqualTo("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL");
    }

    @Test
    void deserialize_fullSearchResponse_extractsSpotifyFromEmbedded() throws Exception {
        String json = """
                {
                    "_embedded": {
                        "events": [
                            {
                                "id": "1A9ZkoaGkePdD04",
                                "name": "Eagles Live at Sphere",
                                "dates": {
                                    "start": {"localDate": "2026-04-10", "dateTime": "2026-04-11T03:30:00Z"},
                                    "status": {"code": "onsale"}
                                },
                                "_embedded": {
                                    "venues": [{"id": "V1", "name": "Sphere"}],
                                    "attractions": [
                                        {
                                            "id": "K8vZ9171ob7",
                                            "name": "Eagles",
                                            "externalLinks": {
                                                "spotify": [{"url": "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL"}]
                                            }
                                        }
                                    ]
                                }
                            }
                        ]
                    },
                    "page": {"size": 1, "totalElements": 1, "totalPages": 1, "number": 0}
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterSearchResponse response = mapper.readValue(json, TicketmasterSearchResponse.class);

        TicketmasterEventResponse event = response.embedded().events().getFirst();
        TicketmasterAttractionResponse attraction = event.embedded().attractions().getFirst();
        assertThat(attraction.externalLinks().get("spotify").getFirst().url())
                .contains("0ECwFtbIWEVNwjlrfc6xoL");
    }

    @Test
    void deserialize_eventWithAllInclusivePricing_parsesTicketing() throws Exception {
        String json = """
                {
                    "id": "TM-AIP-001",
                    "name": "Eagles Live at Sphere",
                    "dates": {
                        "start": {"localDate": "2026-04-10", "dateTime": "2026-04-11T03:00:00Z"},
                        "status": {"code": "onsale"}
                    },
                    "ticketing": {
                        "safeTix": {"enabled": true},
                        "allInclusivePricing": {"enabled": true}
                    },
                    "_embedded": {
                        "venues": [{"id": "V1", "name": "Sphere"}],
                        "attractions": []
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterEventResponse event = mapper.readValue(json, TicketmasterEventResponse.class);

        assertThat(event.isAllInclusivePricing()).isTrue();
        assertThat(event.priceRanges()).isNull();
    }

    @Test
    void deserialize_eventWithDoorsTimes_parsesDoorsTimes() throws Exception {
        String json = """
                {
                    "id": "TM-DOORS-001",
                    "name": "Test Concert",
                    "dates": {
                        "start": {"localDate": "2026-04-10", "dateTime": "2026-04-11T01:00:00Z"},
                        "status": {"code": "onsale"}
                    },
                    "doorsTimes": {
                        "localDate": "2026-04-10",
                        "localTime": "18:30:00",
                        "dateTime": "2026-04-10T22:30:00Z"
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterEventResponse event = mapper.readValue(json, TicketmasterEventResponse.class);

        assertThat(event.doorsTimes()).isNotNull();
        assertThat(event.doorsTimes().localTime()).isEqualTo("18:30:00");
        assertThat(event.doorsTimes().dateTime()).isEqualTo("2026-04-10T22:30:00Z");
    }

    @Test
    void isAllInclusivePricing_givenNullTicketing_returnsFalse() throws Exception {
        String json = """
                {
                    "id": "TM-NULL-001",
                    "name": "No Ticketing",
                    "dates": {
                        "start": {"localDate": "2026-04-10"},
                        "status": {"code": "onsale"}
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterEventResponse event = mapper.readValue(json, TicketmasterEventResponse.class);

        assertThat(event.isAllInclusivePricing()).isFalse();
    }
}
