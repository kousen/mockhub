package com.mockhub.ai.service;

/**
 * Carries the authenticated user's email into tool calls.
 * When set, MCP tools must use this email instead of the caller-supplied userEmail parameter.
 *
 * <p>Two paths set it: website chat ({@code ChatService} pins the logged-in user before invoking
 * the ChatClient) and OAuth2 MCP ({@code McpAuthenticatedEmailFilter} pins the access-token subject
 * for {@code /mcp/**} requests). Under plain {@code X-API-Key} MCP auth nothing sets it, so the
 * parameter is honored as-is.</p>
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
