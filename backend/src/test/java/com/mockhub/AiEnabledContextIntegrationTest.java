package com.mockhub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.mockhub.ai.service.ChatService;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.ai.service.RecommendationService;

/**
 * Boots the full application context with the ai-anthropic profile active
 * (using a dummy API key — no live calls are made).
 *
 * Unit tests mock the ChatClient, so they cannot catch bean-wiring failures:
 * circular dependencies, qualifier ambiguity between the chatClient /
 * plainChatClient / evalJudgeChatClient beans, or conditional-activation
 * mistakes. Only a real context startup with the AI profile active does.
 */
@SpringBootTest(properties = "spring.ai.anthropic.api-key=test-key")
@ActiveProfiles({"test", "mock-payment", "mock-sms", "mock-email", "ai-anthropic"})
@DisplayName("AI-enabled application context")
class AiEnabledContextIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AbstractIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AbstractIntegrationTest.POSTGRES::getPassword);
    }

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("starts with all AI services wired")
    void context_withAiAnthropicProfile_wiresAllAiServices() {
        Assertions.assertNotNull(context.getBean(ChatService.class));
        Assertions.assertNotNull(context.getBean(PricePredictionService.class));
        Assertions.assertNotNull(context.getBean(RecommendationService.class));
    }
}
