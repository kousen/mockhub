package com.mockhub.mcp.config;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
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
    void registeredClientRepository_containsClaudeClient() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        assertNotNull(repository.findByClientId("claude-mcp-client"));
    }

    @Test
    void registeredClientRepository_claudeClientHasCorrectRedirectUris() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
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
    void registeredClientRepository_claudeClientHasRefreshTokenGrant() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
        assertNotNull(client);
        assertTrue(client.getAuthorizationGrantTypes()
                        .contains(AuthorizationGrantType.REFRESH_TOKEN),
                "Client must support refresh_token grant type");
    }

    @Test
    void registeredClientRepository_claudeClientUsesConfiguredAccessTokenTtl() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
        assertNotNull(client);
        Duration accessTokenTtl = client.getTokenSettings().getAccessTokenTimeToLive();
        assertEquals(DEFAULT_ACCESS_TTL, accessTokenTtl);
    }

    @Test
    void registeredClientRepository_claudeClientUsesConfiguredRefreshTokenTtl() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
        assertNotNull(client);
        Duration refreshTokenTtl = client.getTokenSettings().getRefreshTokenTimeToLive();
        assertEquals(DEFAULT_REFRESH_TTL, refreshTokenTtl);
    }

    @Test
    void registeredClientRepository_customTtlsFlowThroughToTokenSettings() {
        McpOAuth2SecurityConfig customConfig =
                new McpOAuth2SecurityConfig(Duration.ofMinutes(5), Duration.ofDays(7));
        RegisteredClientRepository repository = customConfig.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
        assertNotNull(client);
        assertEquals(Duration.ofMinutes(5), client.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofDays(7), client.getTokenSettings().getRefreshTokenTimeToLive());
    }

    @Test
    void registeredClientRepository_claudeClientDoesNotReuseRefreshTokens() {
        RegisteredClientRepository repository = config.registeredClientRepository();

        var client = repository.findByClientId("claude-mcp-client");
        assertNotNull(client);
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
