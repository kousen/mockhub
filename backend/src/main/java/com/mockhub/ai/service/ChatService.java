package com.mockhub.ai.service;

import java.time.Instant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;

@Service
@ConditionalOnBean(ChatClient.class)
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse chat(ChatRequest request) {
        String aiResponse = chatClient.prompt()
                .user(request.message())
                .call()
                .content();

        return new ChatResponse(
                request.conversationId(),
                aiResponse,
                Instant.now()
        );
    }
}
