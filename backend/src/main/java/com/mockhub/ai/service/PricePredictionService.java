package com.mockhub.ai.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;

@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class PricePredictionService {

    private static final Logger log = LoggerFactory.getLogger(PricePredictionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final EventRepository eventRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EvalRunner evalRunner;

    public PricePredictionService(@org.springframework.context.annotation.Lazy ChatClient chatClient,
                                  EventRepository eventRepository,
                                  PriceHistoryRepository priceHistoryRepository,
                                  EvalRunner evalRunner) {
        this.chatClient = chatClient;
        this.eventRepository = eventRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.evalRunner = evalRunner;
    }

    @Transactional(readOnly = true)
    public PricePredictionDto predictPrice(String eventSlug) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        List<PriceHistory> history = priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(event.getId());

        String historyContext = history.stream()
                .limit(10)
                .map(ph -> String.format("price=$%s, daysToEvent=%d", ph.getPrice(), ph.getDaysToEvent()))
                .collect(Collectors.joining("; "));

        String prompt = String.format(
                """
                Analyze the price trend for this event and predict the future price.

                Event: %s
                Current min price: $%s
                Current max price: $%s
                Days until event: %d
                Recent price history (newest first): %s

                Respond with ONLY a JSON object (no markdown, no explanation):
                {"predictedPrice": <number>, "trend": "<RISING|FALLING|STABLE>", "confidence": <0.0-1.0>}
                """,
                event.getName(),
                event.getMinPrice(),
                event.getMaxPrice(),
                ChronoUnit.DAYS.between(Instant.now(), event.getEventDate()),
                historyContext.isEmpty() ? "No history available" : historyContext
        );

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        PricePredictionDto prediction = parsePrediction(aiResponse, eventSlug, event.getMinPrice());

        EvalContext evalContext = EvalContext.forPricePrediction(prediction.predictedPrice(), prediction.currentPrice());
        EvalSummary evalSummary = evalRunner.evaluate(evalContext);
        if (evalSummary.hasCriticalFailure()) {
            log.warn("Price prediction eval failed for slug [{}]: {}", eventSlug.replaceAll("[\r\n]", ""), evalSummary.failures());
            return new PricePredictionDto(eventSlug, event.getMinPrice(), event.getMinPrice(),
                    "STABLE", 0.0, Instant.now());
        }

        return prediction;
    }

    private PricePredictionDto parsePrediction(String aiResponse, String eventSlug, BigDecimal currentPrice) {
        try {
            JsonNode json = MAPPER.readTree(aiResponse.strip());
            BigDecimal predictedPrice = json.get("predictedPrice").decimalValue();
            String trend = json.get("trend").asText();
            double confidence = json.get("confidence").asDouble();

            return new PricePredictionDto(
                    eventSlug,
                    currentPrice,
                    predictedPrice,
                    trend,
                    Math.clamp(confidence, 0.0, 1.0),
                    Instant.now()
            );
        } catch (Exception e) {
            log.warn("Failed to parse AI price prediction response: {}", aiResponse, e);
            return new PricePredictionDto(
                    eventSlug,
                    currentPrice,
                    currentPrice,
                    "STABLE",
                    0.0,
                    Instant.now()
            );
        }
    }
}
