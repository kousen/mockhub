package com.mockhub.tmexplorer;

import java.util.List;
import java.util.Optional;

import com.mockhub.tmexplorer.client.TicketmasterClient;
import com.mockhub.tmexplorer.model.AttractionResponse;
import com.mockhub.tmexplorer.model.EventResponse;
import com.mockhub.tmexplorer.model.SearchResponse;
import com.mockhub.tmexplorer.parser.SpotifyIdExtractor;

/**
 * CLI tool for exploring Ticketmaster Discovery API responses.
 *
 * Usage:
 *   ./gradlew run                        # Search Music events (default)
 *   ./gradlew run --args="keyword Beyoncé"  # Search by keyword
 *   ./gradlew run --args="event vvG1fZ9pBkdiRe"  # Fetch single event
 *   ./gradlew run --args="raw Music"     # Print raw JSON for a Music search
 *
 * Requires TICKETMASTER_API_KEY environment variable.
 */
public class TmExplorer {

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("TICKETMASTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set TICKETMASTER_API_KEY environment variable");
            System.exit(1);
        }

        TicketmasterClient client = new TicketmasterClient(apiKey);
        SpotifyIdExtractor spotifyExtractor = new SpotifyIdExtractor();

        String command = args.length > 0 ? args[0] : "search";
        String argument = args.length > 1 ? args[1] : "Music";
        String country = args.length > 2 ? args[2] : null;

        switch (command) {
            case "search" -> searchAndReport(client, spotifyExtractor, argument, country);
            case "keyword" -> keywordSearch(client, spotifyExtractor, argument, country);
            case "event" -> fetchSingleEvent(client, spotifyExtractor, argument);
            case "raw" -> printRawSearch(client, argument, country);
            case "raw-event" -> printRawEvent(client, argument);
            default -> {
                System.err.println("Unknown command: " + command);
                System.err.println("Commands: search <classification> [countryCode], "
                        + "keyword <query> [countryCode], "
                        + "event <id>, raw <classification> [countryCode], raw-event <id>");
            }
        }
    }

    private static void searchAndReport(TicketmasterClient client,
                                         SpotifyIdExtractor extractor,
                                         String classification,
                                         String country) throws Exception {
        System.out.println("=== Searching Ticketmaster: classification=" + classification
                + (country != null ? ", country=" + country : "") + " ===\n");

        SearchResponse response = country != null
                ? client.searchByCountry(classification, country, 20, 0)
                : client.searchEvents(classification, 20, 0);
        if (response.embedded() == null || response.embedded().events() == null) {
            System.out.println("No events found.");
            return;
        }

        List<EventResponse> events = response.embedded().events();
        System.out.printf("Found %d events (total: %d)%n%n",
                events.size(), response.page().totalElements());

        reportEvents(events, extractor);
    }

    private static void keywordSearch(TicketmasterClient client,
                                       SpotifyIdExtractor extractor,
                                       String keyword,
                                       String country) throws Exception {
        System.out.println("=== Keyword search: " + keyword
                + (country != null ? ", country=" + country : "") + " ===\n");

        SearchResponse response = country != null
                ? client.searchByKeywordAndCountry(keyword, country, 10)
                : client.searchByKeyword(keyword, 10);
        if (response.embedded() == null || response.embedded().events() == null) {
            System.out.println("No events found.");
            return;
        }

        reportEvents(response.embedded().events(), extractor);
    }

    private static void fetchSingleEvent(TicketmasterClient client,
                                          SpotifyIdExtractor extractor,
                                          String eventId) throws Exception {
        System.out.println("=== Fetching event: " + eventId + " ===\n");

        EventResponse event = client.getEvent(eventId);
        reportSingleEvent(event, extractor);
    }

    private static void printRawSearch(TicketmasterClient client,
                                        String classification,
                                        String country) throws Exception {
        String raw = country != null
                ? client.searchEventsRawByCountry(classification, country, 5)
                : client.searchEventsRaw(classification, 5);
        System.out.println(client.prettyPrint(raw));
    }

    private static void printRawEvent(TicketmasterClient client,
                                       String eventId) throws Exception {
        String raw = client.getEventRaw(eventId);
        System.out.println(client.prettyPrint(raw));
    }

    private static void reportEvents(List<EventResponse> events, SpotifyIdExtractor extractor) {
        int withSpotify = 0;
        int withPriceRanges = 0;
        int withAllInclusive = 0;
        int withAttractions = 0;
        int withDoorsTimes = 0;

        for (EventResponse event : events) {
            reportSingleEvent(event, extractor);

            if (event.hasPricing()) {
                withPriceRanges++;
            }
            if (event.isAllInclusivePricing()) {
                withAllInclusive++;
            }
            if (event.doorsTimes() != null) {
                withDoorsTimes++;
            }
            if (event.embedded() != null && event.embedded().attractions() != null
                    && !event.embedded().attractions().isEmpty()) {
                withAttractions++;
                Optional<String> spotifyId = extractor.fromEvent(event);
                if (spotifyId.isPresent()) {
                    withSpotify++;
                }
            }

            System.out.println();
        }

        System.out.println("=== Summary ===");
        System.out.printf("Total events:         %d%n", events.size());
        System.out.printf("With priceRanges:     %d%n", withPriceRanges);
        System.out.printf("All-inclusive pricing: %d (price not in Discovery API)%n", withAllInclusive);
        System.out.printf("No pricing info:      %d%n",
                events.size() - withPriceRanges - withAllInclusive);
        System.out.printf("With attractions:     %d%n", withAttractions);
        System.out.printf("With Spotify IDs:     %d%n", withSpotify);
        System.out.printf("Missing Spotify:      %d (of %d with attractions)%n",
                withAttractions - withSpotify, withAttractions);
        System.out.printf("With doors time:      %d%n", withDoorsTimes);
    }

    private static void reportSingleEvent(EventResponse event, SpotifyIdExtractor extractor) {
        System.out.printf("Event: %s%n", event.name());
        System.out.printf("  ID:       %s%n", event.id());
        System.out.printf("  Date:     %s%n",
                event.dates() != null && event.dates().start() != null
                        ? event.dates().start().localDate() : "N/A");
        System.out.printf("  Pricing:  %s%n", event.pricingStatus());

        // Doors time
        if (event.doorsTimes() != null) {
            System.out.printf("  Doors:    %s%n", event.doorsTimes().localTime());
        }

        // Sales info
        if (event.sales() != null && event.sales().publicSale() != null) {
            EventResponse.PublicSale pub = event.sales().publicSale();
            System.out.printf("  On sale:  %s%n",
                    Boolean.TRUE.equals(pub.startTBD()) ? "TBD"
                            : pub.startDateTime() != null ? pub.startDateTime() : "N/A");
        }

        // Products (parking, VIP, etc.)
        if (event.products() != null && !event.products().isEmpty()) {
            for (EventResponse.Product product : event.products()) {
                System.out.printf("  Product:  %s (%s)%n", product.name(), product.type());
            }
        }

        // Attractions + Spotify
        if (event.embedded() != null && event.embedded().attractions() != null) {
            for (AttractionResponse attraction : event.embedded().attractions()) {
                System.out.printf("  Artist:   %s (TM ID: %s)%n",
                        attraction.name(), attraction.id());

                if (attraction.externalLinks() != null) {
                    System.out.printf("  Links:    %s%n", attraction.externalLinks().keySet());

                    Optional<String> spotifyId = extractor.fromAttraction(attraction);
                    if (spotifyId.isPresent()) {
                        System.out.printf("  Spotify:  %s (valid: %s)%n",
                                spotifyId.get(), extractor.isValidSpotifyId(spotifyId.get()));
                    } else if (attraction.externalLinks().containsKey("spotify")) {
                        System.out.printf("  Spotify:  EXTRACTION FAILED! Raw: %s%n",
                                attraction.externalLinks().get("spotify"));
                    } else {
                        System.out.println("  Spotify:  not in externalLinks");
                    }
                } else {
                    System.out.println("  Links:    null (no externalLinks on attraction)");
                }
            }
        } else {
            System.out.println("  Artists:  NONE (no _embedded.attractions)");
        }
    }
}
