package com.mockhub.ai.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Mock
    private EvalRunner evalRunner;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatClient, evalRunner);
    }

    private void stubChatClient(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    private void stubEvalRunnerPassing() {
        when(evalRunner.evaluate(any(EvalContext.class)))
                .thenReturn(new EvalSummary(List.of(EvalResult.pass("test"))));
    }

    @Test
    @DisplayName("chat - given valid message - returns AI response")
    void chat_givenValidMessage_returnsAiResponse() {
        ChatRequest request = new ChatRequest("What concerts are in New York?", null);
        stubChatClient("Here are some upcoming concerts in New York...");
        stubEvalRunnerPassing();

        ChatResponse response = chatService.chat(request, null);

        assertNotNull(response, "Response should not be null");
        assertEquals("Here are some upcoming concerts in New York...", response.message());
        assertNotNull(response.timestamp(), "Timestamp should be set");
    }

    @Test
    @DisplayName("chat - given message with conversationId - preserves conversationId")
    void chat_givenMessageWithConversationId_preservesConversationId() {
        ChatRequest request = new ChatRequest("Tell me more", 42L);
        stubChatClient("Sure, here's more detail...");
        stubEvalRunnerPassing();

        ChatResponse response = chatService.chat(request, null);

        assertEquals(42L, response.conversationId());
    }

    @Test
    @DisplayName("chat - runs eval conditions after generating response")
    void chat_givenValidMessage_runsEvalConditions() {
        ChatRequest request = new ChatRequest("Tell me about events", null);
        stubChatClient("Here are some events...");
        stubEvalRunnerPassing();

        chatService.chat(request, null);

        verify(evalRunner).evaluate(any(EvalContext.class));
    }

    @Test
    @DisplayName("chat - returns response even when eval fails")
    void chat_givenEvalFailure_returnsResponseAnyway() {
        ChatRequest request = new ChatRequest("Tell me about events", null);
        stubChatClient("Here are some events...");
        when(evalRunner.evaluate(any(EvalContext.class)))
                .thenReturn(new EvalSummary(List.of(
                        EvalResult.fail("grounding",
                                com.mockhub.eval.dto.EvalSeverity.WARNING, "Fabricated data"))));

        ChatResponse response = chatService.chat(request, null);

        assertNotNull(response);
        assertEquals("Here are some events...", response.message());
    }

    @Test
    @DisplayName("chat - given user email - prepends user context to message")
    void chat_givenUserEmail_prependsUserContext() {
        ChatRequest request = new ChatRequest("Buy me a ticket", null);
        stubChatClient("I'll help you purchase a ticket...");
        stubEvalRunnerPassing();

        chatService.chat(request, "ken@example.com");

        verify(requestSpec).user("[User context: logged in as ken@example.com]\n\nBuy me a ticket");
    }

    @Test
    @DisplayName("chat - given null user email - sends original message unchanged")
    void chat_givenNullUserEmail_sendsOriginalMessage() {
        ChatRequest request = new ChatRequest("What concerts are available?", null);
        stubChatClient("Here are the concerts...");
        stubEvalRunnerPassing();

        chatService.chat(request, null);

        verify(requestSpec).user("What concerts are available?");
    }

    @Test
    @DisplayName("chat - given blank user email - sends original message unchanged")
    void chat_givenBlankUserEmail_sendsOriginalMessage() {
        ChatRequest request = new ChatRequest("Show me events", null);
        stubChatClient("Here are the events...");
        stubEvalRunnerPassing();

        chatService.chat(request, "   ");

        verify(requestSpec).user("Show me events");
    }
}
