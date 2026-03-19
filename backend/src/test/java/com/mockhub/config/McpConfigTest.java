package com.mockhub.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.mockhub.mcp.tools.CartTools;
import com.mockhub.mcp.tools.EventTools;
import com.mockhub.mcp.tools.OrderTools;
import com.mockhub.mcp.tools.PricingTools;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class McpConfigTest {

    @Mock
    private EventTools eventTools;

    @Mock
    private PricingTools pricingTools;

    @Mock
    private CartTools cartTools;

    @Mock
    private OrderTools orderTools;

    @Test
    @DisplayName("mcpToolCallbackProvider - creates provider with all tool objects")
    void mcpToolCallbackProvider_createsProviderWithAllToolObjects() {
        McpConfig config = new McpConfig();

        ToolCallbackProvider provider = config.mcpToolCallbackProvider(
                eventTools, pricingTools, cartTools, orderTools);

        assertNotNull(provider, "ToolCallbackProvider should not be null");
    }
}
