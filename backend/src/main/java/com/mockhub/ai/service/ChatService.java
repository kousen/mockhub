package com.mockhub.ai.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;

@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_CONVERSATION_ID = "default";
    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final EvalRunner evalRunner;

    public ChatService(ChatClient chatClient, EvalRunner evalRunner) {
        this.chatClient = chatClient;
        this.evalRunner = evalRunner;
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

        EvalContext evalContext = EvalContext.forChat(aiResponse, request.message());
        EvalSummary evalSummary = evalRunner.evaluate(evalContext);
        if (!evalSummary.allPassed()) {
            log.warn("Chat eval conditions failed: {}", evalSummary.failures());
        }

        return new ChatResponse(
                request.conversationId(),
                aiResponse,
                Instant.now()
        );
    }
}
