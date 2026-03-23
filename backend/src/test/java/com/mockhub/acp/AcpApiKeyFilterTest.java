package com.mockhub.acp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcpApiKeyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    // --- non-ACP paths ---

    @Test
    @DisplayName("doFilterInternal - given non-ACP path - passes through to filter chain")
    void doFilterInternal_givenNonAcpPath_passesThroughToFilterChain() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("test-key");
        when(request.getRequestURI()).thenReturn("/api/v1/events");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // --- blank configured key ---

    @Test
    @DisplayName("doFilterInternal - given blank configured key - rejects ACP request with 401")
    void doFilterInternal_givenBlankConfiguredKey_rejectsAcpRequestWith401() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("");
        when(request.getRequestURI()).thenReturn("/acp/v1/checkout");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    // --- missing API key header ---

    @Test
    @DisplayName("doFilterInternal - given missing API key header - rejects with 401")
    void doFilterInternal_givenMissingApiKeyHeader_rejectsWith401() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("test-key");
        when(request.getRequestURI()).thenReturn("/acp/v1/checkout");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    // --- blank API key header ---

    @Test
    @DisplayName("doFilterInternal - given blank API key header - rejects with 401")
    void doFilterInternal_givenBlankApiKeyHeader_rejectsWith401() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("test-key");
        when(request.getRequestURI()).thenReturn("/acp/v1/catalog");
        when(request.getHeader("X-API-Key")).thenReturn("   ");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    // --- invalid API key ---

    @Test
    @DisplayName("doFilterInternal - given invalid API key - rejects with 401")
    void doFilterInternal_givenInvalidApiKey_rejectsWith401() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("correct-key");
        when(request.getRequestURI()).thenReturn("/acp/v1/checkout");
        when(request.getHeader("X-API-Key")).thenReturn("wrong-key");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    // --- valid API key ---

    @Test
    @DisplayName("doFilterInternal - given valid API key - passes through to filter chain")
    void doFilterInternal_givenValidApiKey_passesThroughToFilterChain() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("test-key");
        when(request.getRequestURI()).thenReturn("/acp/v1/checkout");
        when(request.getHeader("X-API-Key")).thenReturn("test-key");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - given valid API key with whitespace - strips and passes through")
    void doFilterInternal_givenValidApiKeyWithWhitespace_stripsAndPassesThrough() throws ServletException, IOException {
        AcpApiKeyFilter filter = new AcpApiKeyFilter("test-key");
        when(request.getRequestURI()).thenReturn("/acp/v1/checkout");
        when(request.getHeader("X-API-Key")).thenReturn("  test-key  ");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
