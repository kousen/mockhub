package com.mockhub.mcp.config;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.SecurityUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrincipalSanitizingOAuth2AuthorizationServiceTest {

    private static final String EMAIL = "alice@example.com";

    @Mock
    private OAuth2AuthorizationService delegate;

    @Test
    void save_givenSecurityUserPrincipal_replacesPrincipalWithEmailString() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = authorizationWithPrincipal(securityUserAuthentication());

        service.save(authorization);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);
        verify(delegate).save(captor.capture());
        Authentication saved = captor.getValue().getAttribute(Principal.class.getName());
        assertNotNull(saved);
        assertEquals(EMAIL, saved.getPrincipal(), "Principal should be the plain email string");
        assertEquals(EMAIL, saved.getName());
        assertTrue(saved.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")),
                "Authorities must survive sanitization");
    }

    @Test
    void save_givenSecurityUserPrincipal_preservesTokensAndPrincipalName() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = authorizationWithPrincipal(securityUserAuthentication());

        service.save(authorization);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);
        verify(delegate).save(captor.capture());
        OAuth2Authorization saved = captor.getValue();
        assertEquals(EMAIL, saved.getPrincipalName());
        assertNotNull(saved.getRefreshToken(), "Refresh token must survive sanitization");
        assertEquals("refresh-token-value", saved.getRefreshToken().getToken().getTokenValue());
    }

    @Test
    void save_givenNonSecurityUserPrincipal_passesAuthorizationThroughUnchanged() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        Authentication alreadySanitized = UsernamePasswordAuthenticationToken.authenticated(
                EMAIL, null, Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        OAuth2Authorization authorization = authorizationWithPrincipal(alreadySanitized);

        service.save(authorization);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);
        verify(delegate).save(captor.capture());
        assertSame(authorization, captor.getValue(),
                "Already-serializable authorizations should not be rebuilt");
    }

    @Test
    void save_givenNoPrincipalAttribute_passesAuthorizationThroughUnchanged() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .principalName("client-credentials-client")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        service.save(authorization);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);
        verify(delegate).save(captor.capture());
        assertSame(authorization, captor.getValue());
    }

    @Test
    void remove_delegates() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = authorizationWithPrincipal(securityUserAuthentication());

        service.remove(authorization);

        verify(delegate).remove(authorization);
    }

    @Test
    void findById_delegates() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = authorizationWithPrincipal(securityUserAuthentication());
        when(delegate.findById("auth-id")).thenReturn(authorization);

        assertSame(authorization, service.findById("auth-id"));
    }

    @Test
    void findByToken_delegates() {
        PrincipalSanitizingOAuth2AuthorizationService service =
                new PrincipalSanitizingOAuth2AuthorizationService(delegate);
        OAuth2Authorization authorization = authorizationWithPrincipal(securityUserAuthentication());
        when(delegate.findByToken("refresh-token-value", OAuth2TokenType.REFRESH_TOKEN))
                .thenReturn(authorization);

        assertSame(authorization,
                service.findByToken("refresh-token-value", OAuth2TokenType.REFRESH_TOKEN));
    }

    private static Authentication securityUserAuthentication() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$10$hash");
        user.setRoles(Set.of(new Role("ROLE_USER")));
        SecurityUser securityUser = new SecurityUser(user);
        return UsernamePasswordAuthenticationToken.authenticated(
                securityUser, null, securityUser.getAuthorities());
    }

    private static OAuth2Authorization authorizationWithPrincipal(Authentication principal) {
        Instant now = Instant.now();
        return OAuth2Authorization.withRegisteredClient(registeredClient())
                .principalName(EMAIL)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), principal)
                .token(new OAuth2RefreshToken("refresh-token-value", now, now.plus(Duration.ofDays(60))))
                .build();
    }

    private static RegisteredClient registeredClient() {
        return RegisteredClient.withId("test-client")
                .clientId("test-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://claude.ai/api/mcp/auth_callback")
                .build();
    }
}
