package com.mockhub.a2a.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecurityScheme(
        String type,
        String description,
        @JsonProperty("oauth2_metadata_url") String oauth2MetadataUrl
) {}
