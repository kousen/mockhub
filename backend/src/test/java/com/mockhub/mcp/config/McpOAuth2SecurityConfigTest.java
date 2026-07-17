package com.mockhub.mcp.config;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.time.Duration;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpOAuth2SecurityConfigTest {

    private static final Duration DEFAULT_ACCESS_TTL = Duration.ofHours(8);
    private static final Duration DEFAULT_REFRESH_TTL = Duration.ofDays(60);

    private final McpOAuth2SecurityConfig config =
            new McpOAuth2SecurityConfig(DEFAULT_ACCESS_TTL, DEFAULT_REFRESH_TTL);

    private static final JWKSelector RSA_SELECTOR = new JWKSelector(
            new JWKMatcher.Builder().keyType(KeyType.RSA).build());

    @Test
    void jwkSource_givenNoEnvVar_generatesEphemeralRsaKeyPair() throws Exception {
        JWKSource<SecurityContext> jwkSource = config.jwkSource("");

        assertNotNull(jwkSource);
        List<JWK> keys = jwkSource.get(RSA_SELECTOR, null);
        assertEquals(1, keys.size());
        assertNotNull(keys.getFirst().toRSAKey().toRSAPublicKey());
    }

    @Test
    void jwkSource_givenPersistedJwk_loadsFromJson() throws Exception {
        // Generate a key, serialize it, then verify it round-trips
        JWKSource<SecurityContext> ephemeral = config.jwkSource("");
        String jwkJson = ephemeral.get(RSA_SELECTOR, null).getFirst().toJSONString();

        JWKSource<SecurityContext> persisted = config.jwkSource(jwkJson);

        List<JWK> keys = persisted.get(RSA_SELECTOR, null);
        assertEquals(1, keys.size());
        RSAPublicKey originalKey = ephemeral.get(RSA_SELECTOR, null)
                .getFirst().toRSAKey().toRSAPublicKey();
        RSAPublicKey loadedKey = keys.getFirst().toRSAKey().toRSAPublicKey();
        assertEquals(originalKey, loadedKey, "Persisted key should match original");
    }

    @Test
    void jwkSource_givenNullEnvVar_generatesEphemeralKey() throws Exception {
        JWKSource<SecurityContext> jwkSource = config.jwkSource(null);

        assertNotNull(jwkSource);
        List<JWK> keys = jwkSource.get(RSA_SELECTOR, null);
        assertEquals(1, keys.size());
    }

    @Test
    void registeredClientRepository_givenSeedingRace_swallowsDuplicateKeyException() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        // Simulate a concurrent instance winning the insert race: the lookup by id
        // finds no row, and the subsequent insert hits the unique constraint.
        org.mockito.Mockito.when(jdbcTemplate.query(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(),
                        org.mockito.ArgumentMatchers.any(Object[].class)))
                .thenReturn(List.of());
        org.mockito.Mockito.when(jdbcTemplate.update(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(
                                org.springframework.jdbc.core.PreparedStatementSetter.class)))
                .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint"));

        assertNotNull(config.registeredClientRepository(jdbcTemplate),
                "Losing the seeding race must not fail startup");
    }

    @Test
    void claudeRegisteredClient_hasClaudeClientId() {
        var client = config.claudeRegisteredClient();

        assertEquals("claude-mcp-client", client.getClientId());
    }

    @Test
    void claudeRegisteredClient_hasFixedEntityId_soSeedingIsIdempotent() {
        // The JDBC repository's save is an update when the id already exists; with a
        // random id every startup would insert a duplicate client_id row instead.
        assertEquals(config.claudeRegisteredClient().getId(),
                config.claudeRegisteredClient().getId());
        assertEquals("claude-mcp-client", config.claudeRegisteredClient().getId());
    }

    @Test
    void claudeRegisteredClient_hasCorrectRedirectUris() {
        var client = config.claudeRegisteredClient();
        assertNotNull(client);
        assertFalse(client.getRedirectUris().isEmpty());
        // Claude's callback URI must be registered
        assertNotNull(client.getRedirectUris().stream()
                .filter(uri -> uri.contains("claude.ai"))
                .findFirst()
                .orElse(null), "Claude callback URI must be registered");
    }

    @Test
    void authorizationServerSettings_usesConfiguredIssuerUri() {
        String issuerUri = "https://mockhub.kousenit.com";

        AuthorizationServerSettings settings = config.authorizationServerSettings(issuerUri);

        assertEquals(issuerUri, settings.getIssuer());
    }

    @Test
    void claudeRegisteredClient_hasRefreshTokenGrant() {
        var client = config.claudeRegisteredClient();

        assertTrue(client.getAuthorizationGrantTypes()
                        .contains(AuthorizationGrantType.REFRESH_TOKEN),
                "Client must support refresh_token grant type");
    }

    @Test
    void claudeRegisteredClient_usesConfiguredAccessTokenTtl() {
        var client = config.claudeRegisteredClient();

        Duration accessTokenTtl = client.getTokenSettings().getAccessTokenTimeToLive();
        assertEquals(DEFAULT_ACCESS_TTL, accessTokenTtl);
    }

    @Test
    void claudeRegisteredClient_usesConfiguredRefreshTokenTtl() {
        var client = config.claudeRegisteredClient();

        Duration refreshTokenTtl = client.getTokenSettings().getRefreshTokenTimeToLive();
        assertEquals(DEFAULT_REFRESH_TTL, refreshTokenTtl);
    }

    @Test
    void claudeRegisteredClient_customTtlsFlowThroughToTokenSettings() {
        McpOAuth2SecurityConfig customConfig =
                new McpOAuth2SecurityConfig(Duration.ofMinutes(5), Duration.ofDays(7));

        var client = customConfig.claudeRegisteredClient();
        assertEquals(Duration.ofMinutes(5), client.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofDays(7), client.getTokenSettings().getRefreshTokenTimeToLive());
    }

    @Test
    void claudeRegisteredClient_doesNotReuseRefreshTokens() {
        var client = config.claudeRegisteredClient();

        assertFalse(client.getTokenSettings().isReuseRefreshTokens(),
                "Refresh tokens should rotate on each use");
    }

    @Test
    void toMcpRegisteredClient_dynamicClientHasRefreshTokenGrant() {
        var client = config.toMcpRegisteredClient(dynamicAuthorizationCodeRegistration());

        assertTrue(client.getAuthorizationGrantTypes()
                        .contains(AuthorizationGrantType.REFRESH_TOKEN),
                "DCR clients must support refresh_token grant type");
    }

    @Test
    void toMcpRegisteredClient_dynamicClientUsesConfiguredAccessTokenTtl() {
        var client = config.toMcpRegisteredClient(dynamicAuthorizationCodeRegistration());

        assertEquals(DEFAULT_ACCESS_TTL, client.getTokenSettings().getAccessTokenTimeToLive());
    }

    @Test
    void toMcpRegisteredClient_dynamicClientUsesConfiguredRefreshTokenTtl() {
        var client = config.toMcpRegisteredClient(dynamicAuthorizationCodeRegistration());

        assertEquals(DEFAULT_REFRESH_TTL, client.getTokenSettings().getRefreshTokenTimeToLive());
    }

    @Test
    void authorizationServerSettings_usesLocalhostIssuerUri() {
        String issuerUri = "http://localhost:8080";

        AuthorizationServerSettings settings = config.authorizationServerSettings(issuerUri);

        assertEquals(issuerUri, settings.getIssuer());
    }

    private static OAuth2ClientRegistration dynamicAuthorizationCodeRegistration() {
        return OAuth2ClientRegistration.builder()
                .clientName("Codex MCP client")
                .redirectUri("http://127.0.0.1:1455/oauth/callback")
                .grantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                .responseType("code")
                .tokenEndpointAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())
                .build();
    }
}
