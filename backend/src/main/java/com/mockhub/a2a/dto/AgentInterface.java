package com.mockhub.a2a.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentInterface(
        String url,
        @JsonProperty("protocol_binding") String protocolBinding,
        @JsonProperty("protocol_version") String protocolVersion
) {}
