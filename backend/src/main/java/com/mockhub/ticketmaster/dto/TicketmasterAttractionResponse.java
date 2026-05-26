package com.mockhub.ticketmaster.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketmasterAttractionResponse(
        String id,
        String name,
        Map<String, List<ExternalLink>> externalLinks
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalLink(
            String url,
            String id
    ) {
    }

    /**
     * Builder for TicketmasterAttractionResponse. Avoids fragile positional constructors
     * in test code, especially when the nested externalLinks map obscures intent.
     * All fields default to null so callers only set what they need.
     */
    public static final class Builder {
        private String id;
        private String name;
        private Map<String, List<ExternalLink>> externalLinks;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder externalLinks(Map<String, List<ExternalLink>> externalLinks) {
            this.externalLinks = externalLinks;
            return this;
        }

        public TicketmasterAttractionResponse build() {
            return new TicketmasterAttractionResponse(id, name, externalLinks);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
