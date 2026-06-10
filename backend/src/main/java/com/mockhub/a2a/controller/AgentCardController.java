package com.mockhub.a2a.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.a2a.dto.AgentCapabilities;
import com.mockhub.a2a.dto.AgentCard;
import com.mockhub.a2a.dto.AgentInterface;
import com.mockhub.a2a.dto.AgentProvider;
import com.mockhub.a2a.dto.AgentSkill;
import com.mockhub.a2a.dto.SecurityScheme;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "A2A", description = "Agent-to-Agent protocol discovery")
public class AgentCardController {

    private final String baseUrl;

    public AgentCardController(@Value("${mockhub.public-base-url}") String publicBaseUrl) {
        // Strip any trailing slash so we can concatenate paths cleanly.
        this.baseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "A2A Agent Card",
            description = "Returns the A2A Agent Card describing MockHub's capabilities and skills")
    public AgentCard agentCard() {
        return new AgentCard(
                "MockHub",
                "Secondary concert ticket marketplace with agentic commerce support. "
                        + "Search events, browse listings, manage cart, purchase tickets, "
                        + "and manage agent mandates via MCP tools.",
                List.of(new AgentInterface(
                        baseUrl + "/mcp",
                        "mcp/streamable-http",
                        "2025-03-26"
                )),
                new AgentProvider(
                        baseUrl,
                        "MockHub"
                ),
                "1.0.0",
                baseUrl + "/llms.txt",
                new AgentCapabilities(true, false),
                Map.of("oauth2", new SecurityScheme(
                        "oauth2",
                        "OAuth 2.1 with Dynamic Client Registration",
                        baseUrl + "/.well-known/openid-configuration"
                )),
                List.of("text/plain"),
                List.of("text/plain", "application/json"),
                buildSkills(),
                null
        );
    }

    private List<AgentSkill> buildSkills() {
        return List.of(
                new AgentSkill(
                        "ticket-search",
                        "Ticket Search",
                        "Search for events and available ticket listings by query, "
                                + "category, city, date range, price range, and section",
                        List.of("search", "events", "tickets"),
                        List.of(
                                "Find concerts in New York under $150",
                                "Search for orchestra seats under $150"
                        ),
                        null, null
                ),
                new AgentSkill(
                        "ticket-purchase",
                        "Ticket Purchase",
                        "Complete a ticket purchase workflow: add to cart, checkout, "
                                + "and confirm order. Requires an active mandate.",
                        List.of("purchase", "cart", "checkout", "orders"),
                        List.of(
                                "Buy the cheapest ticket that matches my budget",
                                "Add listing 42 to my cart and check out"
                        ),
                        null, null
                ),
                new AgentSkill(
                        "mandate-management",
                        "Mandate Management",
                        "Create, list, validate, and revoke agent mandates that "
                                + "authorize agents to act on behalf of users",
                        List.of("authorization", "mandates", "permissions"),
                        List.of(
                                "Create a purchase mandate with a $200 per-transaction limit",
                                "Show my active mandates"
                        ),
                        null, null
                ),
                new AgentSkill(
                        "price-analysis",
                        "Price Analysis",
                        "Get price history and AI-powered price predictions for events",
                        List.of("pricing", "analytics", "predictions"),
                        List.of("What's the price trend for the top result in this search?"),
                        null, null
                )
        );
    }
}
