package com.mockhub.mcp.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpOAuth2LoginControllerTest {

    private final McpOAuth2LoginController controller = new McpOAuth2LoginController();

    @Test
    void loginPage_returnsHtml() {
        String html = controller.loginPage();

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void loginPage_containsFormWithUsernameAndPassword() {
        String html = controller.loginPage();

        assertTrue(html.contains("name=\"username\""));
        assertTrue(html.contains("name=\"password\""));
        assertTrue(html.contains("method=\"post\""));
    }

    @Test
    void loginPage_postsToOAuth2LoginEndpoint() {
        String html = controller.loginPage();

        assertTrue(html.contains("action=\"/oauth2/login\""));
    }

    @Test
    void loginPage_containsMockHubBranding() {
        String html = controller.loginPage();

        assertTrue(html.contains("MockHub"));
        assertTrue(html.contains("MCP"));
    }

    @Test
    void loginPage_hasViewportMetaTag() {
        String html = controller.loginPage();

        assertTrue(html.contains("viewport"));
        assertTrue(html.contains("width=device-width"));
    }
}
