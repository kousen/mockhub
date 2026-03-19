package com.mockhub.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.EventTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.mcp.tools.PricingTools;

@Configuration
@ConditionalOnProperty(name = "mockhub.mcp.enabled", havingValue = "true")
public class McpConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(EventTools eventTools,
                                                       PricingTools pricingTools,
                                                       CartTools cartTools,
                                                       OrderTools orderTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(eventTools, pricingTools, cartTools, orderTools)
                .build();
    }
}
