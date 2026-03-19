package com.mockhub.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class AiConfig {

    private static final int CHAT_MEMORY_WINDOW_SIZE = 10;

    /**
     * Designates a primary ChatModel when multiple AI providers are on the classpath.
     * Uses @Primary to resolve NoUniqueBeanDefinition when multiple ChatModel beans exist.
     * The actual provider is determined by which AI profile is active — this bean
     * simply marks whichever ChatModel Spring injects as the default.
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(ChatModel chatModel) {
        return chatModel;
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(CHAT_MEMORY_WINDOW_SIZE)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("You are a helpful assistant for MockHub, "
                        + "a secondary concert ticket marketplace. "
                        + "Help users find events, understand pricing, "
                        + "and navigate the platform.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
