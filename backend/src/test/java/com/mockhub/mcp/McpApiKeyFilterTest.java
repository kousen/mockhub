package com.mockhub.mcp;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpApiKeyFilterTest {

    private static final String VALID_API_KEY = "test-mcp-api-key-12345";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private McpApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new McpApiKeyFilter(VALID_API_KEY);
    }

    @Nested
    @DisplayName("Non-MCP paths")
    class NonMcpPaths {

        @Test
        @DisplayName("given request to non-MCP path - passes through filter chain")
        void givenNonMcpPath_passesThroughFilterChain() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/events");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("given request to root path - passes through filter chain")
        void givenRootPath_passesThroughFilterChain() throws Exception {
            when(request.getRequestURI()).thenReturn("/");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("given request to path starting with mcp but not /mcp/ - passes through")
        void givenPathStartingWithMcpButNotMcpSlash_passesThroughFilterChain() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcpanel/dashboard");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("MCP paths with valid API key")
    class McpPathsWithValidKey {

        @Test
        @DisplayName("given MCP request with valid API key - passes through filter chain")
        void givenMcpRequestWithValidKey_passesThroughFilterChain() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcp/sse");
            when(request.getHeader("X-API-Key")).thenReturn(VALID_API_KEY);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("given MCP request with valid API key with whitespace - strips and passes through")
        void givenMcpRequestWithValidKeyWithWhitespace_passesThroughFilterChain() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcp/message");
            when(request.getHeader("X-API-Key")).thenReturn("  " + VALID_API_KEY + "  ");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("MCP paths with missing API key")
    class McpPathsWithMissingKey {

        @Test
        @DisplayName("given MCP request with no API key header - returns 401")
        void givenMcpRequestWithNoKeyHeader_returns401() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcp/sse");
            when(request.getHeader("X-API-Key")).thenReturn(null);
            StringWriter stringWriter = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(filterChain, never()).doFilter(request, response);
            assertTrue(stringWriter.toString().contains("Missing API key"),
                    "Response should mention missing API key");
        }

        @Test
        @DisplayName("given MCP request with blank API key - returns 401")
        void givenMcpRequestWithBlankKey_returns401() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcp/sse");
            when(request.getHeader("X-API-Key")).thenReturn("   ");
            StringWriter stringWriter = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("MCP paths with invalid API key")
    class McpPathsWithInvalidKey {

        @Test
        @DisplayName("given MCP request with wrong API key - returns 401")
        void givenMcpRequestWithWrongKey_returns401() throws Exception {
            when(request.getRequestURI()).thenReturn("/mcp/sse");
            when(request.getHeader("X-API-Key")).thenReturn("wrong-api-key");
            StringWriter stringWriter = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(filterChain, never()).doFilter(request, response);
            assertTrue(stringWriter.toString().contains("Invalid API key"),
                    "Response should mention invalid API key");
        }
    }
}
