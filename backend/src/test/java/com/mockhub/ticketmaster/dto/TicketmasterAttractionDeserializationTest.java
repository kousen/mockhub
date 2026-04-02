package com.mockhub.ticketmaster.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterAttractionDeserializationTest {

    @Test
    void deserialize_attractionWithSpotifyLinks_extractsCorrectly() throws Exception {
        String json = """
                {
                    "id": "K8vZ9171ob7",
                    "name": "Eagles",
                    "externalLinks": {
                        "spotify": [{"url": "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL"}],
                        "youtube": [{"url": "https://youtube.com/test"}]
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterAttractionResponse result = mapper.readValue(json, TicketmasterAttractionResponse.class);

        assertThat(result.name()).isEqualTo("Eagles");
        assertThat(result.externalLinks()).isNotNull();
        assertThat(result.externalLinks()).containsKey("spotify");
        assertThat(result.externalLinks().get("spotify")).hasSize(1);
        assertThat(result.externalLinks().get("spotify").getFirst().url())
                .isEqualTo("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL");
    }

    @Test
    void deserialize_attractionWithMusicbrainzId_extractsBothFields() throws Exception {
        String json = """
                {
                    "id": "K8vZ9171ob7",
                    "name": "Eagles",
                    "externalLinks": {
                        "musicbrainz": [{"id": "f46bd570-5768-462e-b84c-c7c993bbf47e", "url": "https://musicbrainz.org/artist/f46bd570"}]
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterAttractionResponse result = mapper.readValue(json, TicketmasterAttractionResponse.class);

        TicketmasterAttractionResponse.ExternalLink link = result.externalLinks().get("musicbrainz").getFirst();
        assertThat(link.url()).contains("musicbrainz.org");
        assertThat(link.id()).isEqualTo("f46bd570-5768-462e-b84c-c7c993bbf47e");
    }

    @Test
    void deserialize_attractionWithNullExternalLinks_handlesGracefully() throws Exception {
        String json = """
                {
                    "id": "K8vZ9171ob7",
                    "name": "Unknown Artist"
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        TicketmasterAttractionResponse result = mapper.readValue(json, TicketmasterAttractionResponse.class);

        assertThat(result.externalLinks()).isNull();
    }
}
