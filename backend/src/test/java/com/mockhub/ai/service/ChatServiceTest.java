package com.mockhub.ai.service;

import java.time.Instant;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatClient);
    }

    @Test
    @DisplayName("chat - given valid message - returns AI response")
    void chat_givenValidMessage_returnsAiResponse() {
        ChatRequest request = new ChatRequest("What concerts are in New York?", null);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Here are some upcoming concerts in New York...");

        ChatResponse response = chatService.chat(request);

        assertNotNull(response, "Response should not be null");
        assertEquals("Here are some upcoming concerts in New York...", response.message());
        assertNotNull(response.timestamp(), "Timestamp should be set");
    }

    @Test
    @DisplayName("chat - given message with conversationId - preserves conversationId")
    void chat_givenMessageWithConversationId_preservesConversationId() {
        ChatRequest request = new ChatRequest("Tell me more", 42L);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Sure, here's more detail...");

        ChatResponse response = chatService.chat(request);

        assertEquals(42L, response.conversationId());
    }
}
