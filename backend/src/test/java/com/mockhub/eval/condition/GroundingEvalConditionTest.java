package com.mockhub.eval.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroundingEvalCondition")
class GroundingEvalConditionTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private void stubChatClient(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    @Test
    @DisplayName("evaluate - given disabled - returns skip")
    void evaluate_givenDisabled_returnsSkip() {
        GroundingEvalCondition condition = new GroundingEvalCondition(chatClient, false);
        EvalContext context = EvalContext.forChat("Some response", "Some query");

        EvalResult result = condition.evaluate(context);

        assertTrue(result.passed());
        assertTrue(result.message().contains("AI judge disabled"));
    }

    @Test
    @DisplayName("evaluate - given null client - returns skip")
    void evaluate_givenNullClient_returnsSkip() {
        GroundingEvalCondition condition = new GroundingEvalCondition(null, true);
        EvalContext context = EvalContext.forChat("Some response", "Some query");

        EvalResult result = condition.evaluate(context);

        assertTrue(result.passed());
        assertTrue(result.message().contains("No AI provider active"));
    }

    @Test
    @DisplayName("evaluate - given grounded response - returns pass")
    void evaluate_givenGroundedResponse_returnsPass() {
        GroundingEvalCondition condition = new GroundingEvalCondition(chatClient, true);
        stubChatClient("{\"grounded\": true, \"issues\": []}");
        EvalContext context = EvalContext.forChat("Tickets start at $50", "How much are tickets?");

        EvalResult result = condition.evaluate(context);

        assertTrue(result.passed());
        assertEquals("grounding", result.conditionName());
    }

    @Test
    @DisplayName("evaluate - given fabricated content - returns fail")
    void evaluate_givenFabricatedContent_returnsFail() {
        GroundingEvalCondition condition = new GroundingEvalCondition(chatClient, true);
        stubChatClient("{\"grounded\": false, \"issues\": [\"Fabricated event name\"]}");
        EvalContext context = EvalContext.forChat("The Super Bowl has tickets at $10", "Any events?");

        EvalResult result = condition.evaluate(context);

        assertFalse(result.passed());
        assertEquals(EvalSeverity.WARNING, result.severity());
        assertTrue(result.message().contains("Fabricated event name"));
    }

    @Test
    @DisplayName("evaluate - given client exception - returns skip")
    void evaluate_givenClientException_returnsSkip() {
        GroundingEvalCondition condition = new GroundingEvalCondition(chatClient, true);
        when(chatClient.prompt()).thenThrow(new RuntimeException("API timeout"));
        EvalContext context = EvalContext.forChat("Some response", "Some query");

        EvalResult result = condition.evaluate(context);

        assertTrue(result.passed());
        assertTrue(result.message().contains("Judge evaluation failed"));
        assertTrue(result.message().contains("API timeout"));
    }

    @Test
    @DisplayName("evaluate - given malformed JSON - returns skip")
    void evaluate_givenMalformedJson_returnsSkip() {
        GroundingEvalCondition condition = new GroundingEvalCondition(chatClient, true);
        stubChatClient("not json at all");
        EvalContext context = EvalContext.forChat("Some response", "Some query");

        EvalResult result = condition.evaluate(context);

        assertTrue(result.passed());
        assertTrue(result.message().contains("Judge evaluation failed"));
    }

    @Test
    @DisplayName("appliesTo - given chat context - returns true")
    void appliesTo_givenChatContext_returnsTrue() {
        GroundingEvalCondition condition = new GroundingEvalCondition(null, false);
        EvalContext context = EvalContext.forChat("response", "query");

        assertTrue(condition.appliesTo(context));
    }

    @Test
    @DisplayName("appliesTo - given event context - returns false")
    void appliesTo_givenEventContext_returnsFalse() {
        GroundingEvalCondition condition = new GroundingEvalCondition(null, false);
        EvalContext context = EvalContext.forEvent(new com.mockhub.event.entity.Event());

        assertFalse(condition.appliesTo(context));
    }
}
