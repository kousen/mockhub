package com.mockhub.a2a.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentCapabilities(
        Boolean streaming,
        @JsonProperty("push_notifications") Boolean pushNotifications
) {}
