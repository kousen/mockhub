package com.mockhub.eval.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class EvalConfig {

    @Bean("evalJudgeChatClient")
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
    public ChatClient evalJudgeChatClient(@Lazy AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("""
                        You are an evaluation judge for a ticket marketplace AI assistant.
                        Check whether the assistant's response is grounded in the provided context.
                        Look for:
                        1. Fabricated event names that weren't in the question or context
                        2. Made-up prices, dates, or venue names
                        3. Claims about ticket availability without data to support them
                        Respond with ONLY a JSON object (no markdown, no explanation):
                        {"grounded": true, "issues": []}
                        or
                        {"grounded": false, "issues": ["issue 1", "issue 2"]}
                        """)
                .build();
    }
}
