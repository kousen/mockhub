package com.mockhub.mcp.config;

import java.security.Principal;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import com.mockhub.auth.security.SecurityUser;

/**
 * Delegating {@link OAuth2AuthorizationService} that replaces a {@link SecurityUser}
 * principal with its plain email string before persisting.
 *
 * <p>{@code JdbcOAuth2AuthorizationService} serializes the {@code java.security.Principal}
 * attribute to the {@code oauth2_authorization.attributes} column with Jackson, using
 * Spring Security's class allowlist. {@link SecurityUser} is not on that allowlist and
 * has no Jackson-friendly constructor, so persisting it fails. The token refresh flow
 * only needs {@link Authentication#getName()} and the granted authorities to mint new
 * access tokens, both of which survive the swap — so MCP connectors keep refreshing
 * silently across redeploys without the authorization server ever needing the full
 * user object back.</p>
 */
public class PrincipalSanitizingOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationService delegate;

    public PrincipalSanitizingOAuth2AuthorizationService(OAuth2AuthorizationService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(sanitize(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }

    private static OAuth2Authorization sanitize(OAuth2Authorization authorization) {
        Authentication principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof SecurityUser securityUser) {
            Authentication sanitized = UsernamePasswordAuthenticationToken.authenticated(
                    securityUser.getEmail(), null, securityUser.getAuthorities());
            return OAuth2Authorization.from(authorization)
                    .attribute(Principal.class.getName(), sanitized)
                    .build();
        }
        return authorization;
    }
}
