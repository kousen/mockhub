package com.mockhub.ticketmaster.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TicketmasterApiServiceTest {

    private MockRestServiceServer mockServer;
    private TicketmasterApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        service = new TicketmasterApiService(restClient, "test-api-key");
    }

    @Test
    void searchEvents_givenValidResponse_returnsEvents() {
        String responseJson = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "id": "1A9ZkoaGkePdD04",
                        "name": "Eagles Live at Sphere",
                        "dates": {
                          "start": {
                            "localDate": "2026-04-10",
                            "localTime": "20:30:00",
                            "dateTime": "2026-04-11T03:30:00Z",
                            "dateTBD": false,
                            "timeTBA": false
                          },
                          "timezone": "America/Los_Angeles",
                          "status": { "code": "onsale" }
                        },
                        "classifications": [
                          {
                            "primary": true,
                            "segment": { "id": "KZFzniwnSyZfZ7v7nJ", "name": "Music" },
                            "genre": { "id": "1", "name": "Rock" },
                            "subGenre": { "id": "1", "name": "Pop" }
                          }
                        ],
                        "images": [],
                        "priceRanges": [
                          { "type": "standard", "currency": "USD", "min": 75.0, "max": 250.0 }
                        ],
                        "_embedded": {
                          "venues": [
                            {
                              "id": "KovZ917Atbr",
                              "name": "Sphere",
                              "city": { "name": "Las Vegas" },
                              "state": { "stateCode": "NV" },
                              "country": { "countryCode": "US" }
                            }
                          ],
                          "attractions": [
                            {
                              "id": "K8vZ9171ob7",
                              "name": "Eagles",
                              "externalLinks": {
                                "spotify": [
                                  { "url": "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL" }
                                ]
                              }
                            }
                          ]
                        }
                      }
                    ]
                  },
                  "page": { "size": 1, "totalElements": 1, "totalPages": 1, "number": 0 }
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<TicketmasterEventResponse> events = service.searchEvents("music",
                "2026-04-01T00:00:00Z", "2026-10-01T00:00:00Z", 20, 0);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().name()).isEqualTo("Eagles Live at Sphere");
        assertThat(events.getFirst().id()).isEqualTo("1A9ZkoaGkePdD04");
        mockServer.verify();
    }

    @Test
    void searchEvents_givenNoEmbedded_returnsEmptyList() {
        String responseJson = """
                {
                  "page": { "size": 20, "totalElements": 0, "totalPages": 0, "number": 0 }
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<TicketmasterEventResponse> events = service.searchEvents("music",
                "2026-04-01T00:00:00Z", "2026-10-01T00:00:00Z", 20, 0);

        assertThat(events).isEmpty();
        mockServer.verify();
    }

    @Test
    void searchEvents_given429_retriesWithBackoff() {
        String successJson = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "id": "test123",
                        "name": "Test Event",
                        "dates": {
                          "start": { "localDate": "2026-06-01", "dateTime": "2026-06-01T20:00:00Z", "dateTBD": false, "timeTBA": false },
                          "status": { "code": "onsale" }
                        },
                        "classifications": [],
                        "images": []
                      }
                    ]
                  },
                  "page": { "size": 1, "totalElements": 1, "totalPages": 1, "number": 0 }
                }
                """;

        // First request: 429, second request: success
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "1"));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));

        // Override sleep to no-op for test speed
        service = new TicketmasterApiService(
                RestClient.builder().baseUrl("https://app.ticketmaster.com/discovery/v2").build(),
                "test-api-key") {
            @Override
            void sleep(long millis) {
                // no-op for testing
            }
        };

        // Rebuild mock server with new client - use the simpler approach
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        service = new TicketmasterApiService(restClient, "test-api-key") {
            @Override
            void sleep(long millis) {
                // no-op for testing
            }
        };

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "1"));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));

        List<TicketmasterEventResponse> events = service.searchEvents("music",
                "2026-04-01T00:00:00Z", "2026-10-01T00:00:00Z", 20, 0);

        assertThat(events).hasSize(1);
        mockServer.verify();
    }

    @Test
    void searchEvents_givenServerError_throwsException() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.searchEvents("music",
                "2026-04-01T00:00:00Z", "2026-10-01T00:00:00Z", 20, 0))
                .isInstanceOf(org.springframework.web.client.RestClientException.class);
    }

    @Test
    void searchEvents_givenNullResponse_returnsEmptyList() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/events.json")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<TicketmasterEventResponse> events = service.searchEvents("music",
                "2026-04-01T00:00:00Z", "2026-10-01T00:00:00Z", 20, 0);

        assertThat(events).isEmpty();
    }

    @Test
    void constructor_givenBlankApiKey_throwsIllegalState() {
        assertThatThrownBy(() -> new TicketmasterApiService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TICKETMASTER_API_KEY");
    }

    @Test
    void constructor_givenNullApiKey_throwsIllegalState() {
        assertThatThrownBy(() -> new TicketmasterApiService(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseRetryAfter_givenInvalidHeader_returnsExponentialBackoff() {
        long waitMs = service.parseRetryAfter("not-a-number", 1);

        assertThat(waitMs).isEqualTo(2000); // 1000 * 2^1
    }

    @Test
    void parseRetryAfter_givenHeader_returnsMsValue() {
        long waitMs = service.parseRetryAfter("2", 0);

        assertThat(waitMs).isEqualTo(2000);
    }

    @Test
    void parseRetryAfter_givenNullHeader_returnsExponentialBackoff() {
        long waitMs = service.parseRetryAfter(null, 2);

        assertThat(waitMs).isEqualTo(4000); // 1000 * 2^2
    }
}
