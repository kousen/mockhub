package com.mockhub.tmexplorer.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ticketmaster attraction (artist/performer/team).
 *
 * The externalLinks field contains links to external platforms.
 * Ticketmaster returns this as:
 * <pre>
 * "externalLinks": {
 *   "spotify": [{ "url": "https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m" }],
 *   "youtube": [{ "url": "https://www.youtube.com/..." }],
 *   "twitter": [{ "url": "https://twitter.com/..." }],
 *   ...
 * }
 * </pre>
 *
 * Each platform key maps to a list of link objects. We model these as
 * {@code List<ExternalLink>} rather than raw {@code List<Map<String, String>>}
 * to test whether Jackson 3 handles the generic type correctly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttractionResponse(
        String id,
        String name,
        String locale,
        Map<String, List<ExternalLink>> externalLinks,
        List<EventResponse.Classification> classifications,
        List<EventResponse.Image> images
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalLink(
            String url,
            String id
    ) {
    }
}
