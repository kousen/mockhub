package com.mockhub.mcp.controller;

import java.net.URI;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves MCP-suffixed OAuth2 discovery paths.
 *
 * <p>Per RFC 8414 §3.1, an MCP client of a resource hosted at {@code /mcp} constructs
 * the authorization server metadata URL by inserting
 * {@code /.well-known/oauth-authorization-server} between the host and the resource
 * path, yielding {@code /.well-known/oauth-authorization-server/mcp}. Spring
 * Authorization Server registers only the unsuffixed form, so MCP clients (Claude,
 * mcp-remote, Cursor) hit a 404 on their first discovery probe without this controller.
 *
 * <p>The suffixed and unsuffixed forms serve identical metadata in a single-AS
 * deployment, so this controller responds with a 301 redirect. The redirect is
 * permanent — the suffix has no distinct metadata document of its own.
 */
@Controller
@Profile("mcp-oauth2")
public class McpWellKnownController {

    private static final String AS_METADATA = "/.well-known/oauth-authorization-server";

    @GetMapping(AS_METADATA + "/mcp")
    public ResponseEntity<Void> redirectAuthorizationServerMetadata() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(AS_METADATA))
                .build();
    }
}
