package com.mockhub.a2a.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentCard(
        String name,
        String description,
        @JsonProperty("supported_interfaces") List<AgentInterface> supportedInterfaces,
        AgentProvider provider,
        String version,
        @JsonProperty("documentation_url") String documentationUrl,
        AgentCapabilities capabilities,
        @JsonProperty("security_schemes") Map<String, SecurityScheme> securitySchemes,
        @JsonProperty("default_input_modes") List<String> defaultInputModes,
        @JsonProperty("default_output_modes") List<String> defaultOutputModes,
        List<AgentSkill> skills,
        @JsonProperty("icon_url") String iconUrl
) {}
