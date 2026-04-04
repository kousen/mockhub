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
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class McpOAuth2SecurityConfigTest {

    private final McpOAuth2SecurityConfig config = new McpOAuth2SecurityConfig();

    private static final JWKSelector RSA_SELECTOR = new JWKSelector(
            new JWKMatcher.Builder().keyType(KeyType.RSA).build());

    @Test
    void jwkSource_generatesRsaKeyPair() throws Exception {
        JWKSource<SecurityContext> jwkSource = config.jwkSource();

        assertNotNull(jwkSource);
        List<JWK> keys = jwkSource.get(RSA_SELECTOR, null);
        assertEquals(1, keys.size());
        assertNotNull(keys.getFirst().toRSAKey().toRSAPublicKey());
    }

    @Test
    void jwkSource_generatesUniqueKeysPerCall() throws Exception {
        JWKSource<SecurityContext> jwkSource1 = config.jwkSource();
        JWKSource<SecurityContext> jwkSource2 = config.jwkSource();

        RSAPublicKey key1 = jwkSource1.get(RSA_SELECTOR, null)
                .getFirst().toRSAKey().toRSAPublicKey();
        RSAPublicKey key2 = jwkSource2.get(RSA_SELECTOR, null)
                .getFirst().toRSAKey().toRSAPublicKey();

        assertNotNull(key1);
        assertNotNull(key2);
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
    void authorizationServerSettings_usesLocalhostIssuerUri() {
        String issuerUri = "http://localhost:8080";

        AuthorizationServerSettings settings = config.authorizationServerSettings(issuerUri);

        assertEquals(issuerUri, settings.getIssuer());
    }
}
