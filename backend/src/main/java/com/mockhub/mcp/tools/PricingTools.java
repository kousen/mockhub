package com.mockhub.mcp.tools;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.pricing.dto.PriceHistoryPageDto;
import com.mockhub.pricing.service.PriceHistoryService;

@Component
public class PricingTools {

    private static final Logger log = LoggerFactory.getLogger(PricingTools.class);
    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final PriceHistoryService priceHistoryService;
    private final PricePredictionService pricePredictionService;
    private final ObjectMapper objectMapper;

    public PricingTools(PriceHistoryService priceHistoryService,
                        @Nullable PricePredictionService pricePredictionService,
                        ObjectMapper objectMapper) {
        this.priceHistoryService = priceHistoryService;
        this.pricePredictionService = pricePredictionService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get recent MockHub price history snapshots for an event, newest first. "
            + "Returns { snapshots, returned, totalSnapshots, limit }. Snapshots may be stale — "
            + "check the newest recordedAt before treating the series as current. "
            + "Events can have thousands of snapshots; use the limit parameter, not repeated calls, "
            + "to control response size.")
    public String getPriceHistory(
            @ToolParam(description = "Event URL slug to get price history for", required = true) String eventSlug,
            @ToolParam(description = "Maximum snapshots to return, newest first (default 50, max 500)",
                    required = false) Integer limit) {
        try {
            if (eventSlug == null || eventSlug.isBlank()) {
                return errorJson("Event slug is required");
            }
            int effectiveLimit = limit != null ? limit : DEFAULT_HISTORY_LIMIT;
            PriceHistoryPageDto history =
                    priceHistoryService.getRecentByEventSlug(eventSlug.strip(), effectiveLimit);
            return objectMapper.writeValueAsString(history);
        } catch (Exception e) {
            log.error("Error getting price history for '{}': {}", eventSlug, e.getMessage(), e);
            return errorJson("Failed to get price history: " + e.getMessage());
        }
    }

    @Tool(description = "Get an AI-powered MockHub price prediction for an event. "
            + "Returns predicted future price, trend direction (RISING/FALLING/STABLE), and confidence score. "
            + "Requires an AI provider to be active.")
    public String getPricePrediction(
            @ToolParam(description = "Event URL slug to get price prediction for", required = true) String eventSlug) {
        try {
            if (eventSlug == null || eventSlug.isBlank()) {
                return errorJson("Event slug is required");
            }
            if (pricePredictionService == null) {
                return errorJson("Price prediction is not available — no AI provider is active");
            }
            PricePredictionDto prediction = pricePredictionService.predictPrice(eventSlug.strip());
            return objectMapper.writeValueAsString(prediction);
        } catch (Exception e) {
            log.error("Error getting price prediction for '{}': {}", eventSlug, e.getMessage(), e);
            return errorJson("Failed to get price prediction: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
