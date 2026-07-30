package com.mockhub.config;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

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
    @DisplayName("plainChatClient - given model - builds chat client")
    void plainChatClient_givenModel_buildsChatClient() {
        ChatClient plainChatClient = config.plainChatClient(Mockito.mock(AnthropicChatModel.class));

        Assertions.assertNotNull(plainChatClient);
    }

    @Test
    @DisplayName("plainChatClient - called without conversation id - succeeds")
    void plainChatClient_calledWithoutConversationId_succeeds() {
        // Regression for #291: the memory advisor on the shared chatClient rejects
        // calls without a conversation id. The plain client must never carry it.
        AnthropicChatModel model = Mockito.mock(AnthropicChatModel.class);
        Mockito.when(model.getOptions()).thenReturn(AnthropicChatOptions.builder().build());
        Mockito.when(model.call(ArgumentMatchers.any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        ChatClient plainChatClient = config.plainChatClient(model);
        String content = plainChatClient.prompt().user("predict something").call().content();

        Assertions.assertEquals("ok", content);
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
