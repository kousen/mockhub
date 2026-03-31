package com.mockhub.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 600;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ATTRIBUTES_KEY = "attributes";

    private final byte[] signingKey;
    private final ObjectMapper objectMapper;
    private final boolean secureCookie;

    public CookieOAuth2AuthorizationRequestRepository(
            @Value("${mockhub.jwt.secret}") String jwtSecret,
            @Value("${mockhub.cookie.secure:false}") boolean secureCookie,
            ObjectMapper objectMapper) {
        this.signingKey = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.secureCookie = secureCookie;
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookieValue(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }
        try {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("authorizationUri", authorizationRequest.getAuthorizationUri());
            data.put("clientId", authorizationRequest.getClientId());
            data.put("redirectUri", authorizationRequest.getRedirectUri() != null
                    ? authorizationRequest.getRedirectUri() : "");
            data.put("state", authorizationRequest.getState() != null
                    ? authorizationRequest.getState() : "");
            data.put("scopes", String.join(",", authorizationRequest.getScopes()));
            data.put(ATTRIBUTES_KEY, authorizationRequest.getAttributes());
            String json = objectMapper.writeValueAsString(data);
            String signature = sign(json);
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            addCookie(response, payload + "." + signature);
        } catch (Exception e) {
            deleteCookie(response);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        deleteCookie(response);
        return authRequest;
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest getCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    String[] parts = cookie.getValue().split("\\.", 2);
                    if (parts.length != 2) {
                        return null;
                    }
                    String json = new String(Base64.getUrlDecoder().decode(parts[0]),
                            StandardCharsets.UTF_8);
                    String expectedSignature = sign(json);
                    if (!expectedSignature.equals(parts[1])) {
                        return null;
                    }
                    Map<String, Object> data = objectMapper.readValue(json, Map.class);
                    String scopesStr = (String) data.get("scopes");
                    Set<String> scopes = scopesStr != null && !scopesStr.isEmpty()
                            ? Set.of(scopesStr.split(","))
                            : Set.of();
                    String redirectUri = (String) data.get("redirectUri");
                    String state = (String) data.get("state");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attributes = data.get(ATTRIBUTES_KEY) instanceof Map
                            ? (Map<String, Object>) data.get(ATTRIBUTES_KEY)
                            : Map.of();
                    return OAuth2AuthorizationRequest.authorizationCode()
                            .authorizationUri((String) data.get("authorizationUri"))
                            .clientId((String) data.get("clientId"))
                            .redirectUri(redirectUri.isEmpty() ? null : redirectUri)
                            .state(state.isEmpty() ? null : state)
                            .scopes(scopes)
                            .attributes(attrs -> attrs.putAll(attributes))
                            .build();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign OAuth2 state cookie", e);
        }
    }

    private void addCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
