package com.mockhub.eval.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;

import com.mockhub.eval.condition.GroundingEvalCondition;

@Configuration
public class EvalConfig {

    @Bean("evalJudgeChatClient")
    @Lazy
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
    public ChatClient evalJudgeChatClient(AnthropicChatModel anthropicChatModel) {
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

    @Bean
    public GroundingEvalCondition groundingEvalCondition(
            @Nullable @Lazy @Qualifier("evalJudgeChatClient") ChatClient evalJudgeChatClient,
            @Value("${mockhub.eval.ai-judge.enabled:false}") boolean enabled) {
        return new GroundingEvalCondition(evalJudgeChatClient, enabled);
    }
}
