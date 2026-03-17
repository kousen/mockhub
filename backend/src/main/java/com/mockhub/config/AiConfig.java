package com.mockhub.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnBean(ChatModel.class)
public class AiConfig {

    /**
     * Designates a primary ChatModel when multiple AI providers are on the classpath.
     * Spring AI auto-configures a ChatModel bean per provider (Anthropic, OpenAI, Ollama),
     * which causes a NoUniqueBeanDefinition error without a @Primary designation.
     * This bean wraps whichever ChatModel Spring selects first (typically Anthropic
     * when the ai-anthropic profile is active) and marks it as the default.
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(ChatModel chatModel) {
        return chatModel;
    }

    /**
     * Creates a ChatClient configured for MockHub's assistant personality.
     * Uses the primary ChatModel resolved above.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a helpful assistant for MockHub, "
                        + "a secondary concert ticket marketplace. "
                        + "Help users find events, understand pricing, "
                        + "and navigate the platform.")
                .build();
    }
}
