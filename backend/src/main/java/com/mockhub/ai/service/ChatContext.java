package com.mockhub.ai.service;

/**
 * Carries the authenticated user's email into tool calls during website chat.
 * When set, MCP tools must use this email instead of the LLM-provided userEmail parameter.
 * External MCP requests do not set this context, so the parameter is honored as-is.
 */
public final class ChatContext {

    private static final ThreadLocal<String> authenticatedEmail = new ThreadLocal<>();

    private ChatContext() {
    }

    public static void setAuthenticatedEmail(String email) {
        authenticatedEmail.set(email);
    }

    public static String getAuthenticatedEmail() {
        return authenticatedEmail.get();
    }

    public static void clear() {
        authenticatedEmail.remove();
    }

    /**
     * Returns the effective email for a tool call: the authenticated email if
     * the request came from the website chat, otherwise the parameter email
     * from the external MCP client.
     */
    public static String resolveEmail(String paramEmail) {
        String authenticated = authenticatedEmail.get();
        if (authenticated != null) {
            return authenticated;
        }
        if (paramEmail == null || paramEmail.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        return paramEmail.strip();
    }
}
