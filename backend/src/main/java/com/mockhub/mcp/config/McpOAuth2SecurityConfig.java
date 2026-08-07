package com.mockhub.mcp.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.mcp.McpAuthenticatedEmailFilter;
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

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(McpOAuth2SecurityConfig.class);
    private static final String OAUTH2_LOGIN_PATH = "/oauth2/login";
    private static final String OAUTH2_AUTHORIZED_PATH = "/oauth2/authorized";

    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    /**
     * Token lifetimes are configurable so different policies can be demonstrated
     * (e.g. a 5-minute access token to make the silent-refresh flow visible).
     * Refresh tokens rotate on use, so {@code refreshTokenTtl} is a sliding window:
     * an actively-used connector never re-authenticates, an idle one expires.
     * Spending authority is bounded by mandate limits and expiry, not token lifetime,
     * which is why a generous refresh window is acceptable here.
     */
    public McpOAuth2SecurityConfig(
            @Value("${mockhub.mcp.oauth2.access-token-ttl:8h}") Duration accessTokenTtl,
            @Value("${mockhub.mcp.oauth2.refresh-token-ttl:60d}") Duration refreshTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

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
        http.with(McpAuthorizationServerConfigurer.mcpAuthorizationServer()
                .authorizationServer(authz -> authz.clientRegistrationEndpoint(registration ->
                        registration.authenticationProviders(
                                this::customizeClientRegistrationProviders))),
                Customizer.withDefaults());

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
                request -> OAUTH2_LOGIN_PATH.equals(request.getServletPath())
                        || OAUTH2_AUTHORIZED_PATH.equals(request.getServletPath())));

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(OAUTH2_LOGIN_PATH, OAUTH2_AUTHORIZED_PATH).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(OAUTH2_LOGIN_PATH)
                        .loginProcessingUrl(OAUTH2_LOGIN_PATH)
                        .defaultSuccessUrl(OAUTH2_AUTHORIZED_PATH, false))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(OAUTH2_LOGIN_PATH))
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
            UserRepository userRepository,
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
                // Pin the authenticated token subject onto the tool-call thread so MCP tools act
                // strictly as the logged-in user, not whatever userEmail the agent supplies. Runs
                // after authentication (populated by the bearer token filter) and before
                // authorization, so the SecurityContext is available.
                .addFilterBefore(new McpAuthenticatedEmailFilter(userRepository), AuthorizationFilter.class)
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
     * <p>If {@code MCP_OAUTH2_JWK} environment variable is set, the RSA key pair is
     * loaded from it (JSON JWK format). This ensures tokens survive Railway redeploys —
     * without it, every redeploy generates a new key pair and invalidates all existing
     * tokens. Claude mobile syncs connectors from desktop and cannot force a fresh
     * token exchange, so stale tokens cause persistent 401 failures.</p>
     *
     * <p>If the env var is not set, an ephemeral key pair is generated (suitable for
     * dev/test where redeploy token invalidation is acceptable).</p>
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${MCP_OAUTH2_JWK:}") String jwkJson) {
        RSAKey rsaKey;
        if (jwkJson != null && !jwkJson.isBlank()) {
            rsaKey = parsePersistedJwk(jwkJson);
            log.info("Loaded MCP OAuth2 RSA key from MCP_OAUTH2_JWK environment variable (kid={})",
                    rsaKey.getKeyID());
        } else {
            KeyPair keyPair = generateRsaKey();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
            log.warn("MCP_OAUTH2_JWK not set — using ephemeral RSA key (tokens will not survive redeploys)");
        }
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private RSAKey parsePersistedJwk(String jwkJson) {
        try {
            return RSAKey.parse(jwkJson);
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(
                    "Failed to parse MCP_OAUTH2_JWK — must be a valid RSA JWK JSON object", e);
        }
    }

    /**
     * JDBC-backed client repository so DCR registrations survive Railway redeploys.
     *
     * <p>Claude and Codex register via DCR. With the previous in-memory repository,
     * every redeploy forgot their {@code client_id}, so token refresh failed with
     * {@code invalid_client} and connectors demanded re-authentication daily.</p>
     *
     * <p>The pre-registered Claude client is seeded on startup with a fixed entity id,
     * making {@code save()} an idempotent create-or-update — TTL config changes are
     * re-applied on restart without duplicating rows.</p>
     */
    @Bean
    @DependsOnDatabaseInitialization
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
        try {
            repository.save(claudeRegisteredClient());
        } catch (DuplicateKeyException e) {
            // Two instances starting against a fresh database can race the initial
            // insert; the row exists either way, so the loser just moves on.
            log.info("Claude MCP client already seeded by a concurrent instance");
        }
        return repository;
    }

    /**
     * Pre-registered OAuth2 client for Claude's connector.
     *
     * <p>Claude's custom connector uses DCR to register dynamically, but having a
     * pre-registered client ensures the authorization server is functional even before
     * DCR occurs. The redirect URI matches Claude's expected callback endpoint.</p>
     */
    RegisteredClient claudeRegisteredClient() {
        return RegisteredClient.withId("claude-mcp-client")
                .clientId("claude-mcp-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://claude.ai/api/mcp/auth_callback")
                .redirectUri("http://localhost:6274/oauth/callback")
                .redirectUri("http://127.0.0.1:6274/oauth/callback")
                .tokenSettings(mcpTokenSettings())
                .build();
    }

    /**
     * JDBC-backed authorization service so refresh tokens survive Railway redeploys.
     *
     * <p>Refresh tokens are opaque server-side tokens — unlike the JWT access tokens
     * (which only need the persisted signing key), they live in this store. Without
     * persistence, the first silent refresh after a redeploy failed and Claude/Codex
     * showed "Authentication expired. Reconnect."</p>
     *
     * <p>Wrapped in {@link PrincipalSanitizingOAuth2AuthorizationService} because the
     * JDBC service serializes the login principal with Jackson's security allowlist,
     * which rejects MockHub's custom {@code SecurityUser}.</p>
     */
    @Bean
    @DependsOnDatabaseInitialization
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new PrincipalSanitizingOAuth2AuthorizationService(
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository));
    }

    /**
     * JDBC-backed consent service for completeness. MCP flows are consent-free (no
     * scopes requested), so this table stays empty in practice, but if a client ever
     * requests scopes the recorded consent survives redeploys like everything else.
     */
    @Bean
    @DependsOnDatabaseInitialization
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    private void customizeClientRegistrationProviders(
            List<AuthenticationProvider> authenticationProviders) {
        authenticationProviders.stream()
                .filter(OAuth2ClientRegistrationAuthenticationProvider.class::isInstance)
                .map(OAuth2ClientRegistrationAuthenticationProvider.class::cast)
                .forEach(provider -> provider.setRegisteredClientConverter(
                        this::toMcpRegisteredClient));
    }

    RegisteredClient toMcpRegisteredClient(OAuth2ClientRegistration clientRegistration) {
        Converter<OAuth2ClientRegistration, RegisteredClient> defaultConverter =
                new OAuth2ClientRegistrationRegisteredClientConverter();
        RegisteredClient registeredClient = defaultConverter.convert(clientRegistration);
        if (registeredClient == null) {
            throw new IllegalStateException("Failed to convert dynamic MCP client registration");
        }
        return applyMcpClientDefaults(registeredClient);
    }

    RegisteredClient applyMcpClientDefaults(RegisteredClient registeredClient) {
        return RegisteredClient.from(registeredClient)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(mcpTokenSettings())
                .build();
    }

    private TokenSettings mcpTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(accessTokenTtl)
                .refreshTokenTimeToLive(refreshTokenTtl)
                .reuseRefreshTokens(false)
                .build();
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
