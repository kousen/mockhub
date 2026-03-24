package com.mockhub.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.EventTools;
import com.mockhub.mcp.tools.MandateTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.mcp.tools.PricingTools;

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
    public ChatClient chatClient(AnthropicChatModel anthropicChatModel,
                                  ChatMemory chatMemory,
                                  EventTools eventTools,
                                  PricingTools pricingTools,
                                  CartTools cartTools,
                                  OrderTools orderTools,
                                  MandateTools mandateTools) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("""
                        You are a helpful assistant for MockHub, \
                        a secondary concert ticket marketplace. \
                        Help users find events, understand pricing, \
                        and purchase tickets on behalf of users.

                        You have access to tools that can search events, \
                        get event details, list ticket prices, check \
                        price history, manage shopping carts, and \
                        complete purchases.

                        When a user asks to buy tickets, use findTickets to \
                        search, then use mandate tools before any purchase \
                        mutation. For website chat purchases, the fixed agentId \
                        is `mockhub-web-chat`. First check listMandates for the \
                        user's email. If no suitable PURCHASE mandate exists for \
                        `mockhub-web-chat`, create one with a conservative \
                        spend limit that fits the user's request, or a $2000 \
                        default when the user did not specify a budget. Then use \
                        addToCart with both agentId and mandateId, checkout to \
                        create the order, and confirmOrder to complete it. The \
                        user's email is available from their login session.

                        When mentioning events, always include a markdown link \
                        using the event's URL slug. The format is: \
                        [Event Name](/events/{slug}). For example: \
                        [Taylor Swift - Eras Tour](/events/taylor-swift-eras-tour-8). \
                        Event slugs are lowercase with hyphens, often ending with a number.

                        When mentioning the events page, link to [Browse Events](/events). \
                        Keep responses concise and helpful.""")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(ToolCallbacks.from(eventTools, pricingTools,
                        cartTools, orderTools, mandateTools))
                .build();
    }
}
