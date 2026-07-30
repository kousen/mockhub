package com.mockhub.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.EventTools;
import com.mockhub.mcp.tools.MandateTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.mcp.tools.PricingTools;

@DisplayName("AiConfig")
class AiConfigTest {

    private final AiConfig config = new AiConfig();

    @Test
    @DisplayName("chatMemory - returns configured memory")
    void chatMemory_returnsConfiguredMemory() {
        ChatMemory chatMemory = config.chatMemory();

        Assertions.assertNotNull(chatMemory);
    }

    @Test
    @DisplayName("chatClient - given model and tools - builds chat client")
    void chatClient_givenModelAndTools_buildsChatClient() {
        ChatClient chatClient = config.chatClient(
                Mockito.mock(AnthropicChatModel.class),
                config.chatMemory(),
                Mockito.mock(EventTools.class),
                Mockito.mock(PricingTools.class),
                Mockito.mock(CartTools.class),
                Mockito.mock(OrderTools.class),
                Mockito.mock(MandateTools.class));

        Assertions.assertNotNull(chatClient);
    }
}
