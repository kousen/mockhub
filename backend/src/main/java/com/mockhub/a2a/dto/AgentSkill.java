package com.mockhub.a2a.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentSkill(
        String id,
        String name,
        String description,
        List<String> tags,
        List<String> examples,
        @JsonProperty("input_modes") List<String> inputModes,
        @JsonProperty("output_modes") List<String> outputModes
) {}
