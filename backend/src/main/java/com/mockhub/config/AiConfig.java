package com.mockhub.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Creates a ChatClient from whatever ChatModel is auto-configured by the active
     * Spring AI profile (ai-anthropic, ai-openai, or ai-ollama). Each profile brings
     * its own ChatModel bean via Spring AI auto-configuration.
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
