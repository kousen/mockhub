package com.mockhub.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnBean(ChatModel.class)
public class AiConfig {

    private static final int CHAT_MEMORY_WINDOW_SIZE = 10;

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
     * Creates a ChatMemory backed by in-memory storage with a sliding window
     * of recent messages. The window size limits how many messages are retained
     * per conversation to keep prompts focused and token usage bounded.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(CHAT_MEMORY_WINDOW_SIZE)
                .build();
    }

    /**
     * Creates a ChatClient configured for MockHub's assistant personality.
     * Includes a MessageChatMemoryAdvisor that automatically manages conversation
     * history, retrieving past messages from memory and including them in prompts.
     */
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
