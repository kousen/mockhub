package com.mockhub.mcp.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves a minimal HTML login form for the OAuth2 authorization_code flow.
 *
 * <p>When Claude's connector (or any MCP client) initiates an authorization_code grant,
 * the authorization server redirects the user here to authenticate. After login,
 * the user is redirected back to the authorization endpoint to complete the grant.</p>
 *
 * <p>This login page is separate from the React SPA's login route to avoid conflicts
 * with client-side routing. It lives at {@code /oauth2/login} rather than {@code /login}.</p>
 */
@Controller
@Profile("mcp-oauth2")
public class McpOAuth2LoginController {

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
                        }
                        h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }
                        p { color: #64748b; font-size: 0.875rem; margin-bottom: 1.5rem; }
                        label { display: block; font-size: 0.875rem; font-weight: 500; margin-bottom: 0.25rem; }
                        input[type="text"], input[type="password"] {
                            width: 100%;
                            padding: 0.5rem 0.75rem;
                            border: 1px solid #e2e8f0;
                            border-radius: 6px;
                            font-size: 0.875rem;
                            margin-bottom: 1rem;
                        }
                        input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,0.2); }
                        button {
                            width: 100%;
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
                        .error { color: #ef4444; font-size: 0.8rem; margin-bottom: 1rem; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>MockHub</h1>
                        <p>Sign in to authorize MCP access</p>
                        <form method="post" action="/oauth2/login">
                            <label for="username">Email</label>
                            <input type="text" id="username" name="username" required autofocus>
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" required>
                            <button type="submit">Sign in</button>
                        </form>
                    </div>
                </body>
                </html>
                """;
    }
}
