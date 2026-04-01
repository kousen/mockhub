package com.mockhub.ticketmaster.service;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.mockhub.ticketmaster.dto.TicketmasterSearchResponse;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterDeserializationTest {

    @Test
    void deserialize_realMusicResponse_succeeds() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        ClassPathResource resource = new ClassPathResource("ticketmaster-music-response.json");
        String json = resource.getContentAsString(StandardCharsets.UTF_8);

        TicketmasterSearchResponse response = mapper.readValue(json, TicketmasterSearchResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.embedded()).isNotNull();
        assertThat(response.embedded().events()).isNotEmpty();
    }
}
