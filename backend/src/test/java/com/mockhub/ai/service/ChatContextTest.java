package com.mockhub.ai.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatContextTest {

    @AfterEach
    void tearDown() {
        ChatContext.clear();
    }

    @Test
    @DisplayName("set and get authenticated email")
    void setAndGetAuthenticatedEmail() {
        ChatContext.setAuthenticatedEmail("user@example.com");

        assertEquals("user@example.com", ChatContext.getAuthenticatedEmail());
    }

    @Test
    @DisplayName("get returns null when not set")
    void getReturnsNullWhenNotSet() {
        assertNull(ChatContext.getAuthenticatedEmail());
    }

    @Test
    @DisplayName("clear removes the authenticated email")
    void clearRemovesAuthenticatedEmail() {
        ChatContext.setAuthenticatedEmail("user@example.com");
        ChatContext.clear();

        assertNull(ChatContext.getAuthenticatedEmail());
    }

    @Test
    @DisplayName("resolveEmail returns authenticated email when set")
    void resolveEmail_givenChatContext_returnsAuthenticatedEmail() {
        ChatContext.setAuthenticatedEmail("real@example.com");

        assertEquals("real@example.com", ChatContext.resolveEmail("attacker@example.com"));
    }

    @Test
    @DisplayName("resolveEmail returns parameter email when no context")
    void resolveEmail_givenNoChatContext_returnsParameterEmail() {
        assertEquals("param@example.com", ChatContext.resolveEmail("param@example.com"));
    }

    @Test
    @DisplayName("resolveEmail strips whitespace from parameter email")
    void resolveEmail_givenWhitespace_stripsParameterEmail() {
        assertEquals("param@example.com", ChatContext.resolveEmail("  param@example.com  "));
    }

    @Test
    @DisplayName("resolveEmail throws on null parameter when no context")
    void resolveEmail_givenNullAndNoContext_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ChatContext.resolveEmail(null));
    }

    @Test
    @DisplayName("resolveEmail throws on blank parameter when no context")
    void resolveEmail_givenBlankAndNoContext_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ChatContext.resolveEmail("   "));
    }
}
