package com.mockhub.mcp.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * OAuth 2.1 security configuration for MCP endpoints.
 *
 * <p>Active only when both {@code mcp-oauth2} profile and {@code mockhub.mcp.enabled=true}
 * are set. Replaces the {@link com.mockhub.mcp.McpApiKeyFilter} API key authentication
 * with OAuth 2.1 Bearer token validation and Dynamic Client Registration (DCR).</p>
 *
 * <p>Creates two SecurityFilterChain beans:
 * <ul>
 *   <li>Authorization server chain (highest precedence) — handles OAuth2 token, authorize,
 *       JWKS, and DCR endpoints</li>
 *   <li>MCP resource server chain — validates Bearer tokens on {@code /mcp/**}</li>
 * </ul>
 * The existing SecurityConfig chain handles all other paths and is unaffected.</p>
 */
@Configuration
@Profile("mcp-oauth2")
@ConditionalOnProperty(name = "mockhub.mcp.enabled", havingValue = "true")
public class McpOAuth2SecurityConfig {

    /**
     * Authorization server SecurityFilterChain.
     *
     * <p>Handles OAuth2 endpoints: {@code /oauth2/authorize}, {@code /oauth2/token},
     * {@code /oauth2/jwks}, {@code /.well-known/oauth-authorization-server},
     * and DCR at {@code /connect/register}.</p>
     *
     * <p>Uses {@link McpAuthorizationServerConfigurer} which configures DCR with open
     * registration (any client can register), token generation with resource identifier
     * audience claims, and consent-free flows when no scopes are requested.</p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        http.with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), Customizer.withDefaults());

        // Scope this chain to OAuth2 authorization server endpoints + login page.
        // Must be set after the MCP configurer applies, because it configures
        // OAuth2AuthorizationServerConfigurer which registers the endpoint matchers.
        http.oauth2AuthorizationServer(Customizer.withDefaults());
        OAuth2AuthorizationServerConfigurer authzConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
        // Include the login page in this chain's matcher so the form POST is processed
        // by the formLogin filter in this chain (not the default stateless chain).
        http.securityMatcher(new OrRequestMatcher(
                authzConfigurer.getEndpointsMatcher(),
                request -> "/oauth2/login".equals(request.getServletPath())));

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/oauth2/login")
                        .loginProcessingUrl("/oauth2/login"))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/login"))
                .build();
    }

    /**
     * MCP resource server SecurityFilterChain.
     *
     * <p>Protects {@code /mcp/**} endpoints by requiring a valid OAuth2 Bearer token.
     * Tokens are validated as JWTs signed by this application's authorization server.</p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 5)
    public SecurityFilterChain mcpResourceServerFilterChain(
            HttpSecurity http,
            JwtDecoder mcpJwtDecoder,
            @Value("${mockhub.mcp.oauth2.issuer-uri}") String issuerUri) throws Exception {
        return http
                .securityMatcher("/mcp/**", "/.well-known/oauth-protected-resource",
                        "/.well-known/oauth-protected-resource/mcp")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/.well-known/oauth-protected-resource",
                                "/.well-known/oauth-protected-resource/mcp").permitAll()
                        .anyRequest().authenticated())
                .with(McpServerOAuth2Configurer.mcpServerOAuth2(), mcp -> {
                    mcp.authorizationServer(issuerUri);
                    mcp.resourcePath("/mcp");
                    mcp.jwtDecoder(mcpJwtDecoder);
                    mcp.validateAudienceClaim(true);
                })
                .csrf(csrf -> csrf.disable())
                .build();
    }

    /**
     * JWT decoder for MCP resource server, using the same JWK source as the
     * authorization server. This avoids a network call to the issuer URI at startup,
     * which is necessary when both servers are embedded in the same application.
     */
    @Bean
    public JwtDecoder mcpJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    /**
     * JWK source for signing OAuth2 access tokens.
     *
     * <p>Generates an ephemeral RSA key pair at startup. On Railway redeploys,
     * the key changes and existing tokens become invalid — this is acceptable
     * because DCR clients re-register and obtain new tokens.</p>
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Pre-registered OAuth2 client for Claude's connector.
     *
     * <p>Claude's custom connector uses DCR to register dynamically, but having a
     * pre-registered client ensures the authorization server is functional even before
     * DCR occurs. The redirect URI matches Claude's expected callback endpoint.</p>
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient claudeClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("claude-mcp-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("https://claude.ai/api/mcp/auth_callback")
                .redirectUri("http://localhost:6274/oauth/callback")
                .redirectUri("http://127.0.0.1:6274/oauth/callback")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(claudeClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${mockhub.mcp.oauth2.issuer-uri}") String issuerUri) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate RSA key pair", exception);
        }
    }
}
