package com.mockhub.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class AiConfig {

    private static final int CHAT_MEMORY_WINDOW_SIZE = 10;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(CHAT_MEMORY_WINDOW_SIZE)
                .build();
    }

    @Bean
    public ChatClient chatClient(AnthropicChatModel anthropicChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("""
                        You are a helpful assistant for MockHub, \
                        a secondary concert ticket marketplace. \
                        Help users find events, understand pricing, \
                        and navigate the platform.

                        When mentioning events, always include a markdown link \
                        using the event's URL slug. The format is: \
                        [Event Name](/events/{slug}). For example: \
                        [Taylor Swift - Eras Tour](/events/taylor-swift-eras-tour-8). \
                        Event slugs are lowercase with hyphens, often ending with a number.

                        When mentioning the events page, link to [Browse Events](/events). \
                        Keep responses concise and helpful.""")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
