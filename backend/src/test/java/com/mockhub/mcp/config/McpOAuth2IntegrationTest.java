package com.mockhub.mcp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the MCP OAuth 2.1 security configuration.
 *
 * <p>Verifies that when the {@code mcp-oauth2} profile is active:
 * <ul>
 *   <li>The OAuth2 authorization server metadata endpoint works</li>
 *   <li>The MCP endpoint requires a Bearer token (401 without)</li>
 *   <li>The OAuth2 login page is served</li>
 *   <li>Existing API endpoints remain unaffected</li>
 * </ul>
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
@ActiveProfiles({"test", "mock-payment", "mock-sms", "mock-email", "mcp-oauth2"})
@AutoConfigureTestRestTemplate
class McpOAuth2IntegrationTest {

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

    @Nested
    @DisplayName("Dynamic Client Registration")
    class DynamicClientRegistration {

        @Test
        @DisplayName("DCR response includes refresh token grant")
        void dcrResponse_includesRefreshTokenGrant() throws Exception {
            JsonNode registration = registerClient("codex-mcp-client");

            assertTrue(arrayContains(registration.get("grant_types"), "refresh_token"),
                    "DCR response should advertise refresh_token grant: " + registration);
        }

        @Test
        @DisplayName("DCR client credentials token uses MCP access token TTL")
        void clientCredentialsToken_usesMcpAccessTokenTtl() throws Exception {
            JsonNode registration = registerClient("codex-mcp-client-token");
            String clientId = registration.get("client_id").asText();
            String clientSecret = registration.get("client_secret").asText();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/oauth2/token", new HttpEntity<>(body, headers), String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "Token endpoint should accept dynamically registered client: " + response.getBody());
            JsonNode token = objectMapper.readTree(response.getBody());
            assertTrue(token.get("expires_in").asLong() > 28_000,
                    "DCR client access token should use the 8-hour MCP TTL: " + token);
        }
    }

    @Nested
    @DisplayName("Authorization Server Metadata")
    class AuthorizationServerMetadata {

        @Test
        @DisplayName("well-known endpoint returns OAuth2 metadata")
        void wellKnownEndpoint_returnsOAuth2Metadata() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/.well-known/oauth-authorization-server", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            JsonNode metadata = objectMapper.readTree(response.getBody());
            assertNotNull(metadata.get("issuer"));
            assertNotNull(metadata.get("token_endpoint"));
            assertNotNull(metadata.get("authorization_endpoint"));
        }

        @Test
        @DisplayName("JWKS endpoint returns key set")
        void jwksEndpoint_returnsKeySet() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/oauth2/jwks", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            JsonNode jwks = objectMapper.readTree(response.getBody());
            assertNotNull(jwks.get("keys"));
            assertTrue(jwks.get("keys").isArray());
            assertTrue(jwks.get("keys").size() > 0);
        }
    }

    @Nested
    @DisplayName("MCP Endpoint Protection")
    class McpEndpointProtection {

        @Test
        @DisplayName("MCP endpoint without token returns 401")
        void mcpEndpoint_withoutToken_returns401() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/mcp/", HttpMethod.POST, request, String.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("OAuth2 Login Page")
    class OAuth2LoginPage {

        @Test
        @DisplayName("login page is accessible")
        void loginPage_isAccessible() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/oauth2/login", String.class);

            // The login page should be served (may redirect or serve directly)
            assertTrue(response.getStatusCode().is2xxSuccessful()
                            || response.getStatusCode().is3xxRedirection(),
                    "Login page should be accessible, got: " + response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Existing Endpoints Unaffected")
    class ExistingEndpoints {

        @Test
        @DisplayName("public API endpoints still work without auth")
        void publicEndpoints_stillWorkWithoutAuth() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/v1/categories", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("protected API endpoints still require JWT")
        void protectedEndpoints_stillRequireJwt() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/v1/orders", String.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    private JsonNode registerClient(String clientName) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = objectMapper.createObjectNode();
        request.put("client_name", clientName);
        request.put("token_endpoint_auth_method", "client_secret_basic");
        request.putArray("grant_types").add("client_credentials");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/oauth2/register", new HttpEntity<>(request.toString(), headers), String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "DCR endpoint should register clients: " + response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private static boolean arrayContains(JsonNode array, String expectedValue) {
        if (array == null || !array.isArray()) {
            return false;
        }
        for (JsonNode value : array) {
            if (expectedValue.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }
}
