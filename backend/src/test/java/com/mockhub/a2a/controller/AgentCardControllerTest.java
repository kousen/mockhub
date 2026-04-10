package com.mockhub.a2a.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentCardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AgentCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /.well-known/agent.json - returns 200 with agent card")
    void agentCard_returns200WithCard() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.name").value("MockHub"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.provider.organization").value("MockHub"))
                .andExpect(jsonPath("$.capabilities.streaming").value(true))
                .andExpect(jsonPath("$.skills.length()").value(4))
                .andExpect(jsonPath("$.skills[0].id").value("ticket-search"))
                .andExpect(jsonPath("$.skills[1].id").value("ticket-purchase"))
                .andExpect(jsonPath("$.skills[2].id").value("mandate-management"))
                .andExpect(jsonPath("$.skills[3].id").value("price-analysis"));
    }

    @Test
    @DisplayName("GET /.well-known/agent.json - is publicly accessible (no auth)")
    void agentCard_publicAccess_returns200() throws Exception {
        // No authentication set — endpoint should be publicly accessible
        // via /.well-known/** permitAll() in SecurityConfig
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MockHub"));
    }

    @Test
    @DisplayName("GET /.well-known/agent.json - includes supported interfaces")
    void agentCard_includesSupportedInterfaces() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(jsonPath("$.supported_interfaces.length()").value(1))
                .andExpect(jsonPath("$.supported_interfaces[0].protocol_binding").value("mcp/streamable-http"))
                .andExpect(jsonPath("$.supported_interfaces[0].url").value("https://mockhub.kousenit.com/mcp"));
    }

    @Test
    @DisplayName("GET /.well-known/agent.json - includes security schemes")
    void agentCard_includesSecuritySchemes() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(jsonPath("$.security_schemes.oauth2.type").value("oauth2"));
    }

    @Test
    @DisplayName("GET /.well-known/agent.json - skills have examples")
    void agentCard_skillsHaveExamples() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(jsonPath("$.skills[0].examples.length()").value(2))
                .andExpect(jsonPath("$.skills[0].examples[0]").value("Find jazz concerts in New York"));
    }

    @Test
    @DisplayName("GET /.well-known/agent.json - uses snake_case field names")
    void agentCard_usesSnakeCaseFieldNames() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(jsonPath("$.supported_interfaces").exists())
                .andExpect(jsonPath("$.default_input_modes").exists())
                .andExpect(jsonPath("$.default_output_modes").exists())
                .andExpect(jsonPath("$.documentation_url").exists())
                .andExpect(jsonPath("$.security_schemes").exists());
    }
}
