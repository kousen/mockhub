package com.mockhub.mcp;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class McpSessionRecoveryFilterTest {

    private final McpSessionRecoveryFilter filter = new McpSessionRecoveryFilter();

    @Test
    @DisplayName("doFilter - given session-not-found response - returns 404")
    void doFilter_givenSessionNotFoundResponse_returns404() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) res;
            httpResponse.setStatus(200);
            httpResponse.setContentType("application/json");
            PrintWriter writer = httpResponse.getWriter();
            writer.write("{\"jsonRpcError\":{\"code\":-32603,\"message\":\"Session not found: abc-123\"}}");
            writer.flush();
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("Session expired"));
    }

    @Test
    @DisplayName("doFilter - given normal MCP response - passes through unchanged")
    void doFilter_givenNormalResponse_passesThroughUnchanged() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) res;
            httpResponse.setStatus(200);
            httpResponse.setContentType("application/json");
            PrintWriter writer = httpResponse.getWriter();
            writer.write("{\"jsonrpc\":\"2.0\",\"result\":{\"tools\":[]},\"id\":1}");
            writer.flush();
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("tools"));
    }

    @Test
    @DisplayName("doFilter - given non-MCP path - skips filter entirely")
    void doFilter_givenNonMcpPath_skipsFilter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) res;
            httpResponse.setStatus(200);
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("doFilter - given non-200 MCP response - passes through unchanged")
    void doFilter_givenNon200Response_passesThroughUnchanged() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) res;
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json");
            PrintWriter writer = httpResponse.getWriter();
            writer.write("{\"error\":\"Unauthorized\"}");
            writer.flush();
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
    }
}
