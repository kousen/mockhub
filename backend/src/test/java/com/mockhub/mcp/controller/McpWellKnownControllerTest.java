package com.mockhub.mcp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class McpWellKnownControllerTest {

    private final McpWellKnownController controller = new McpWellKnownController();

    @Test
    void redirectAuthorizationServerMetadata_returns301_toCanonicalPath() {
        ResponseEntity<Void> response = controller.redirectAuthorizationServerMetadata();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/.well-known/oauth-authorization-server");
    }
}
