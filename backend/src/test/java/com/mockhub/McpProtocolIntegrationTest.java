package com.mockhub;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the MCP protocol over Streamable HTTP transport.
 * Sends raw JSON-RPC requests to /mcp and parses SSE responses to verify
 * the full agentic commerce tool discovery works through the actual protocol.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mockhub.mcp.enabled=true",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration," +
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration," +
                        "org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration," +
                        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration"
        }
)
@ActiveProfiles({"test", "mock-payment", "mock-sms", "mock-email"})
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpProtocolIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("mockhub")
                .withUsername("mockhub")
                .withPassword("mockhub");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MCP_API_KEY = "mockhub-dev-key";
    private static String sessionId;

    private HttpHeaders mcpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "text/event-stream, application/json");
        headers.set("X-API-Key", MCP_API_KEY);
        if (sessionId != null) {
            headers.set("Mcp-Session-Id", sessionId);
        }
        return headers;
    }

    private ObjectNode jsonRpcRequest(String method, int id) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", id);
        return request;
    }

    private ObjectNode jsonRpcRequest(String method, int id, ObjectNode params) {
        ObjectNode request = jsonRpcRequest(method, id);
        request.set("params", params);
        return request;
    }

    /**
     * Parse the JSON-RPC result from an SSE response body.
     * SSE format: "id: ...\nevent: message\ndata: {json}\n\n"
     * May contain multiple data lines for a single event.
     */
    private JsonNode parseSSEResponse(String body) throws Exception {
        StringBuilder dataBuilder = new StringBuilder();
        for (String line : body.split("\n")) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).strip();
                dataBuilder.append(data);
            }
        }
        String data = dataBuilder.toString();
        if (!data.isEmpty()) {
            return objectMapper.readTree(data);
        }
        // If not SSE, try parsing as plain JSON
        return objectMapper.readTree(body);
    }

    @Test
    @Order(1)
    @DisplayName("MCP initialize - returns server capabilities and session ID")
    void mcpInitialize_returnsCapabilities() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "test-client");
        clientInfo.put("version", "1.0");
        params.set("clientInfo", clientInfo);
        params.put("protocolVersion", "2025-03-26");

        ObjectNode request = jsonRpcRequest("initialize", 1, params);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), mcpHeaders()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Initialize should return 200, body: " + response.getBody());

        // Capture session ID from response headers
        List<String> sessionHeaders = response.getHeaders().get("Mcp-Session-Id");
        assertNotNull(sessionHeaders, "Response should include Mcp-Session-Id header");
        assertFalse(sessionHeaders.isEmpty(), "Session ID should not be empty");
        sessionId = sessionHeaders.getFirst();

        JsonNode body = parseSSEResponse(response.getBody());
        assertNotNull(body.get("result"), "Should have result field");
        assertNotNull(body.get("result").get("serverInfo"), "Should include server info");
    }

    @Test
    @Order(2)
    @DisplayName("MCP tools/list - returns all 21 registered tools")
    void mcpToolsList_returnsAllTools() throws Exception {
        ObjectNode request = jsonRpcRequest("tools/list", 2);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), mcpHeaders()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "tools/list should return 200, body: " + response.getBody());

        JsonNode body = parseSSEResponse(response.getBody());
        JsonNode tools = body.get("result").get("tools");
        assertNotNull(tools, "Should return tools array");
        assertTrue(tools.isArray(), "Tools should be an array");
        assertTrue(tools.size() >= 21, "Should have at least 21 tools, got " + tools.size());

        // Verify key agentic commerce tools are present
        boolean hasFindTickets = false;
        boolean hasAddToCart = false;
        boolean hasCheckout = false;
        boolean hasCreateMandate = false;
        boolean hasGetBestMandate = false;
        boolean hasGetCalendarEntry = false;
        boolean hasRefreshCart = false;
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            switch (name) {
                case "findTickets" -> hasFindTickets = true;
                case "addToCart" -> hasAddToCart = true;
                case "checkout" -> hasCheckout = true;
                case "createMandate" -> hasCreateMandate = true;
                case "getBestMandate" -> hasGetBestMandate = true;
                case "getCalendarEntry" -> hasGetCalendarEntry = true;
                case "refreshCart" -> hasRefreshCart = true;
                default -> { }
            }
        }
        assertTrue(hasFindTickets, "Should include findTickets tool");
        assertTrue(hasAddToCart, "Should include addToCart tool");
        assertTrue(hasCheckout, "Should include checkout tool");
        assertTrue(hasCreateMandate, "Should include createMandate tool");
        assertTrue(hasGetBestMandate, "Should include getBestMandate tool");
        assertTrue(hasGetCalendarEntry, "Should include getCalendarEntry tool");
        assertTrue(hasRefreshCart, "Should include refreshCart tool");
    }

    @Test
    @Order(3)
    @DisplayName("MCP tools/call findTickets - returns listings via protocol")
    void mcpCallFindTickets_returnsListings() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "findTickets");
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("query", "concert");
        arguments.put("maxResults", 5);
        params.set("arguments", arguments);

        ObjectNode request = jsonRpcRequest("tools/call", 3, params);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), mcpHeaders()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "findTickets should return 200, body: " + response.getBody());

        JsonNode body = parseSSEResponse(response.getBody());
        assertNotNull(body.get("result"), "Should have result");
        JsonNode content = body.get("result").get("content");
        assertNotNull(content, "Should have content array");
        assertTrue(content.isArray() && !content.isEmpty(), "Content should not be empty");

        String toolResult = content.get(0).get("text").asText();
        assertNotNull(toolResult, "Tool should return text content");
    }

    @Test
    @Order(4)
    @DisplayName("MCP tools/call searchEvents - returns paginated events")
    void mcpCallSearchEvents_returnsPaginatedEvents() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "searchEvents");
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("page", 0);
        arguments.put("size", 5);
        params.set("arguments", arguments);

        ObjectNode request = jsonRpcRequest("tools/call", 4, params);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), mcpHeaders()), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "searchEvents should return 200, body: " + response.getBody());

        JsonNode body = parseSSEResponse(response.getBody());
        JsonNode content = body.get("result").get("content");
        assertNotNull(content, "Should have content");
        String toolResult = content.get(0).get("text").asText();
        assertTrue(toolResult.contains("content"), "Should contain paginated content");
    }

    @Test
    @Order(5)
    @DisplayName("MCP rejects requests without API key")
    void mcpWithoutApiKey_rejectsRequest() {
        ObjectNode request = jsonRpcRequest("initialize", 99);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "text/event-stream, application/json");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), headers), String.class);

        assertTrue(response.getStatusCode().isError(),
                "Request without API key should fail, got: " + response.getStatusCode());
    }

    @Test
    @Order(6)
    @DisplayName("MCP rejects requests with wrong API key")
    void mcpWithWrongApiKey_rejectsRequest() {
        ObjectNode request = jsonRpcRequest("initialize", 100);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "text/event-stream, application/json");
        headers.set("X-API-Key", "wrong-key");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/mcp", new HttpEntity<>(request.toString(), headers), String.class);

        assertTrue(response.getStatusCode().isError(),
                "Request with wrong API key should fail, got: " + response.getStatusCode());
    }
}
