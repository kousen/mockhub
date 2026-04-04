package com.mockhub.mcp.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves HTML pages for the OAuth2 authorization_code flow used by MCP clients.
 *
 * <p>When an MCP client (Claude, Cursor, etc.) initiates an authorization_code grant,
 * the authorization server redirects the user to {@code /oauth2/login} to authenticate.
 * After login, the user is redirected back to the authorization endpoint, which issues
 * an authorization code and redirects to the client's callback URL.</p>
 *
 * <p>These pages are separate from the React SPA to avoid conflicts with client-side
 * routing. They live under {@code /oauth2/} rather than the SPA's route namespace.</p>
 */
@Controller
@Profile("mcp-oauth2")
public class McpOAuth2LoginController {

    private static final String SHARED_STYLES = """
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            background: #f8fafc;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                            color: #1e293b;
                        }
                        .card {
                            background: white;
                            border-radius: 12px;
                            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                            padding: 2rem;
                            width: 100%;
                            max-width: 400px;
                            text-align: center;
                        }
                        h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }
                        p { color: #64748b; font-size: 0.875rem; margin-bottom: 1.5rem; }
                        a { color: #3b82f6; text-decoration: none; }
                        a:hover { text-decoration: underline; }
            """;

    @GetMapping(value = "/oauth2/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String loginPage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>MockHub — MCP Authorization</title>
                    <style>
                %s
                        label { display: block; font-size: 0.875rem; font-weight: 500; margin-bottom: 0.25rem; text-align: left; }
                        input[type="text"], input[type="password"] {
                            width: 100%%;
                            padding: 0.5rem 0.75rem;
                            border: 1px solid #e2e8f0;
                            border-radius: 6px;
                            font-size: 0.875rem;
                            margin-bottom: 1rem;
                        }
                        input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,0.2); }
                        button {
                            width: 100%%;
                            padding: 0.625rem;
                            background: #3b82f6;
                            color: white;
                            border: none;
                            border-radius: 6px;
                            font-size: 0.875rem;
                            font-weight: 500;
                            cursor: pointer;
                        }
                        button:hover { background: #2563eb; }
                        .note { color: #94a3b8; font-size: 0.75rem; margin-top: 1.5rem; line-height: 1.4; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>MockHub</h1>
                        <p>Sign in to authorize AI agent access</p>
                        <form method="post" action="/oauth2/login">
                            <label for="username">Email</label>
                            <input type="text" id="username" name="username" required autofocus>
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" required>
                            <button type="submit">Authorize</button>
                        </form>
                        <p class="note">This authorizes an AI agent (Claude, Cursor, etc.) to use
                            MockHub tools on your behalf. To use the MockHub website,
                            <a href="/">go to the main site</a>.</p>
                    </div>
                </body>
                </html>
                """.formatted(SHARED_STYLES);
    }

    @GetMapping(value = "/oauth2/authorized", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String authorizedPage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>MockHub — Authorization Complete</title>
                    <style>
                %s
                        .check { font-size: 3rem; margin-bottom: 1rem; }
                        .note { color: #94a3b8; font-size: 0.75rem; margin-top: 1rem; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="check">&#10003;</div>
                        <h1>Authorization Complete</h1>
                        <p>Your AI agent now has access to MockHub. You can close this tab.</p>
                        <p class="note"><a href="/">Go to MockHub</a></p>
                    </div>
                </body>
                </html>
                """.formatted(SHARED_STYLES);
    }
}
