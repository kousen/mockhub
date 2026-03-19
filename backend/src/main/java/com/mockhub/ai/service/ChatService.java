package com.mockhub.ai.service;

import java.time.Instant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;

@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class ChatService {

    private static final String DEFAULT_CONVERSATION_ID = "default";
    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? String.valueOf(request.conversationId())
                : DEFAULT_CONVERSATION_ID;

        String aiResponse = chatClient.prompt()
                .user(request.message())
                .advisors(advisorSpec -> advisorSpec.param(
                        CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();

        return new ChatResponse(
                request.conversationId(),
                aiResponse,
                Instant.now()
        );
    }
}
