package com.mockhub.mcp.config;

import java.security.Principal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.SecurityUser;
import com.mockhub.lifecycle.LifecycleCleanupService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that OAuth2 client registrations and authorizations are persisted to
 * PostgreSQL rather than held in memory, so MCP connector sessions (Claude Desktop,
 * Codex) survive Railway redeploys (issue #266).
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
class McpOAuth2PersistenceIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private LifecycleCleanupService cleanupService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("pre-registered Claude client is seeded into the database")
    void claudeClient_isSeededIntoDatabase() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = 'claude-mcp-client'",
                Integer.class);

        assertEquals(1, count, "Claude client must be persisted, not held in memory");
    }

    @Test
    @DisplayName("DCR-registered clients are persisted to the database")
    void dcrClient_isPersistedToDatabase() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = objectMapper.createObjectNode();
        request.put("client_name", "persistence-test-client");
        request.put("token_endpoint_auth_method", "client_secret_basic");
        request.putArray("grant_types").add("client_credentials");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/oauth2/register", new HttpEntity<>(request.toString(), headers), String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "DCR should succeed: " + response.getBody());
        JsonNode registration = objectMapper.readTree(response.getBody());
        String clientId = registration.get("client_id").asText();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = ?",
                Integer.class, clientId);
        assertEquals(1, count, "DCR client must survive a restart, so it must be in the database");
    }

    @Test
    @DisplayName("authorization with SecurityUser principal and refresh token round-trips through Postgres")
    void authorizationWithSecurityUserPrincipal_roundTripsThroughPostgres() {
        String refreshTokenValue = "persistence-test-refresh-" + UUID.randomUUID();
        OAuth2Authorization authorization = authorizationWithRefreshToken(
                refreshTokenValue, Instant.now().plus(Duration.ofDays(60)));

        authorizationService.save(authorization);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?",
                Integer.class, authorization.getId());
        assertEquals(1, count, "Authorization must be persisted, not held in memory");

        OAuth2Authorization loaded = authorizationService.findByToken(
                refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);
        assertNotNull(loaded, "Refresh token lookup must work after persistence round-trip");
        assertEquals("student@mockhub.com", loaded.getPrincipalName());
        Authentication principal = loaded.getAttribute(Principal.class.getName());
        assertNotNull(principal, "Principal attribute must deserialize from the database");
        assertEquals("student@mockhub.com", principal.getName(),
                "Refresh flow reads the stored principal name to mint new tokens");

        authorizationService.remove(loaded);
        assertNull(authorizationService.findByToken(refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN));
    }

    @Test
    @DisplayName("lifecycle cleanup deletes expired authorizations but keeps live ones")
    void lifecycleCleanup_deletesExpiredAuthorizations_keepsLiveOnes() {
        String expiredToken = "expired-refresh-" + UUID.randomUUID();
        String liveToken = "live-refresh-" + UUID.randomUUID();
        OAuth2Authorization expired = authorizationWithRefreshToken(
                expiredToken, Instant.now().minus(Duration.ofDays(1)));
        OAuth2Authorization live = authorizationWithRefreshToken(
                liveToken, Instant.now().plus(Duration.ofDays(60)));
        authorizationService.save(expired);
        authorizationService.save(live);
        // Sanity check: JdbcOAuth2AuthorizationService stores expiry as timestamptz
        Timestamp expiry = jdbcTemplate.queryForObject(
                "SELECT refresh_token_expires_at FROM oauth2_authorization WHERE id = ?",
                Timestamp.class, expired.getId());
        assertNotNull(expiry);

        cleanupService.runCleanup();

        Integer expiredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?",
                Integer.class, expired.getId());
        Integer liveCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?",
                Integer.class, live.getId());
        assertEquals(0, expiredCount, "Expired authorizations should be cleaned up");
        assertEquals(1, liveCount, "Live authorizations must not be touched by cleanup");

        authorizationService.remove(live);
    }

    private OAuth2Authorization authorizationWithRefreshToken(String tokenValue, Instant expiresAt) {
        RegisteredClient claudeClient = registeredClientRepository.findByClientId("claude-mcp-client");
        assertNotNull(claudeClient);
        return OAuth2Authorization.withRegisteredClient(claudeClient)
                .principalName("student@mockhub.com")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), securityUserAuthentication())
                .token(new OAuth2RefreshToken(tokenValue, expiresAt.minus(Duration.ofDays(60)), expiresAt))
                .build();
    }

    private static Authentication securityUserAuthentication() {
        User user = new User();
        user.setEmail("student@mockhub.com");
        user.setPasswordHash("$2a$10$hash");
        user.setRoles(Set.of(new Role("ROLE_USER")));
        SecurityUser securityUser = new SecurityUser(user);
        return UsernamePasswordAuthenticationToken.authenticated(
                securityUser, null, securityUser.getAuthorities());
    }
}
