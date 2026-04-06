# Agentic Commerce in MockHub

MockHub implements agentic commerce — the ability for AI agents to search for, evaluate, and purchase concert tickets on behalf of users. This document covers the architecture, protocols, and teaching connections.

## The Three Layers

Agentic commerce in MockHub is organized into three layers, each independently valuable:

```
┌─────────────────────────────────────────────┐
│  Layer 3: ACP Endpoints (Protocol)          │
│  /acp/v1/checkout — interoperable API       │
├─────────────────────────────────────────────┤
│  Layer 2: Mandates (Authorization)          │
│  EvalCondition — what can this agent do?    │
├─────────────────────────────────────────────┤
│  Layer 1: MCP Tools (Capabilities)          │
│  searchEvents, findTickets, addToCart, ...   │
└─────────────────────────────────────────────┘
```

**Layer 1** gives agents the tools to act. **Layer 2** constrains what they're allowed to do. **Layer 3** makes the system interoperable with other agent platforms.

---

## Layer 1: MCP Tools — The Agent's Capabilities

### Tool Inventory

MockHub exposes 20 MCP tools across 5 tool classes:

| Tool Class | Tools | Purpose |
|---|---|---|
| **EventTools** | `searchEvents`, `getEventDetail`, `getEventListings`, `getFeaturedEvents`, `getListingDetail`, `findTickets` | Discovery and search |
| **PricingTools** | `getPriceHistory`, `getPricePrediction` | Price intelligence |
| **CartTools** | `getCart`, `addToCart`, `removeFromCart`, `clearCart` | Shopping cart management |
| **OrderTools** | `checkout`, `confirmOrder`, `getOrder`, `listOrders` | Order lifecycle |
| **MandateTools** | `createMandate`, `revokeMandate`, `listMandates`, `validateMandate` | Agent authorization |

### The Complete Purchase Flow

An agent can now execute a full purchase on behalf of a user:

```
1. findTickets(query="Taylor Swift", city="NYC", dateFrom="2026-04-01T00:00:00Z",
               dateTo="2026-04-30T23:59:59Z", maxPrice=200)
   → Returns matching listings with event/date metadata, sorted by price

2. addToCart(userEmail="buyer@example.com", listingId=42,
             agentId="shopping-agent-1", mandateId="abc-123")
   → Eval conditions check: event in future, listing active
   → MandateCondition checks agent identity, mandate proof, and purchase authority
   → Returns cart with any warnings

3. checkout(userEmail="buyer@example.com", paymentMethod="mock",
            agentId="shopping-agent-1", mandateId="abc-123")
   → Validates listings, reserves tickets, creates PENDING order
   → Returns OrderDto with order number

4. confirmOrder(userEmail="buyer@example.com", orderNumber="MH-20260323-0001",
                agentId="shopping-agent-1", mandateId="abc-123")
   → Routes through PaymentService
   → Marks order CONFIRMED only after successful payment confirmation
   → Records mandate spend once, updates ticket status to SOLD, triggers SMS + email
   → Returns confirmed OrderDto
```

### Key Design: `findTickets` — The Compound Search Tool

Before `findTickets`, agents needed three round-trips to find a ticket (search events → get detail → get listings). `findTickets` collapses this into a single call with rich filtering:

```
findTickets(
    query: "jazz",           // text search on event name/artist
    category: "jazz",        // category filter
    city: "New York",        // venue city
    dateFrom: "2026-04-01T00:00:00Z",
    dateTo: "2026-04-30T23:59:59Z",
    minPrice: 50,            // price floor
    maxPrice: 150,           // price ceiling
    section: "Orchestra",    // section name filter
    maxResults: 5            // limit results
)
```

This is an example of **agent-ergonomic API design** — reducing round-trips and letting agents express intent in a single call. Traditional REST APIs are designed for human-driven UIs with navigation; agent APIs should support goal-directed actions.

### Eval Conditions as Guardrails

Every `addToCart` call passes through the eval conditions framework:

| Condition | Severity | What It Checks |
|---|---|---|
| `EventInFutureCondition` | CRITICAL | Event hasn't occurred, status is ACTIVE |
| `ListingActiveCondition` | CRITICAL | Listing status is ACTIVE |
| `SpendingLimitCondition` | WARNING | Cart subtotal ≤ $2,000 (configurable) |
| `PricePlausibilityCondition` | WARNING | Price isn't anomalously high/low |
| `MandateCondition` | CRITICAL | Agent has authorization (Layer 2) |

CRITICAL failures block the action. WARNING failures are included in the response as advisory messages — the agent (or its orchestrator) can decide whether to proceed.

---

## Layer 2: Mandates — Agent Authorization

### The Problem

Without mandates, any authenticated agent can buy anything for anyone. That's fine for demos but unacceptable for real commerce. The fundamental question: **when an autonomous agent initiates a purchase, how do we verify the user authorized it?**

### The Mandate Concept

A **mandate** is a record of what an agent is authorized to do on behalf of a specific user. It defines:

- **Scope**: `BROWSE` (read-only) or `PURCHASE` (can buy). PURCHASE subsumes BROWSE.
- **Spending limits**: Per-transaction max and cumulative total budget.
- **Category restrictions**: Only buy tickets in specific categories (e.g., "jazz,rock").
- **Event restrictions**: Only buy tickets to specific events.
- **Expiration**: Optional time-bound authorization.

### Mandate Lifecycle

```
1. User grants mandate to agent
   createMandate(agentId="shopping-agent-1", userEmail="buyer@example.com",
                 scope="PURCHASE", maxSpendPerTransaction=200, maxSpendTotal=1000,
                 allowedCategories="jazz,rock")

2. Agent acts within mandate
   findTickets(...) → addToCart(...) → checkout(...)
   Each action is checked against the active mandate via MandateCondition

3. User revokes mandate
   revokeMandate(mandateId="abc-123")
```

### How MandateCondition Works

`MandateCondition` is an `EvalCondition` that activates when `agentId` is present in the `EvalContext`:

1. Require explicit `agentId` for autonomous actions and `mandateId` for purchase actions
2. Look up the specific active, non-expired mandate for the agent+user+mandate tuple
3. Check the scope matches the action (BROWSE for reads, PURCHASE for buys)
4. Check per-transaction spending limit
5. Check cumulative spending limit (tracks `totalSpent`)
6. Check category and event restrictions

If any check fails, the condition returns a CRITICAL failure — the action is blocked.

### Connection to AP2

Google's Agent Payments Protocol (AP2) defines "mandates" as digitally signed statements of agent authority. MockHub's mandates serve the same purpose but are enforced through the eval conditions framework rather than cryptographic signatures.

The conceptual mapping:

| AP2 Concept | MockHub Implementation |
|---|---|
| Digitally signed mandate | Database record checked by `MandateCondition` |
| Scope (capabilities) | `scope` field (BROWSE/PURCHASE) |
| Spending limits | `maxSpendPerTransaction`, `maxSpendTotal` |
| Revocation | `revokeMandate()` sets status to REVOKED |
| Expiration | `expiresAt` checked at query time |

### Connection to Design by Contract

Mandates are a specialization of the Design by Contract (DbC) pattern that already powers MockHub's eval conditions:

- **Preconditions** → Is the agent authorized? Is the mandate active? Is the budget sufficient?
- **Postconditions** → After purchase, has `totalSpent` been updated?
- **Invariants** → An agent never exceeds its spending limit. A revoked mandate never authorizes actions.

This is the same DbC → eval conditions → mandates progression that Nate Jones's contextual stewardship framework describes: formalized judgment at system boundaries.

---

## Layer 3: ACP Endpoints — Protocol Interoperability

### What is ACP?

The Agentic Commerce Protocol (ACP) is an open standard codeveloped by Stripe and OpenAI that enables programmatic commerce between AI agents and businesses. It defines a RESTful interface for checkout operations that any ACP-compatible agent can use.

### MockHub's ACP Implementation

MockHub exposes ACP-compatible endpoints at `/acp/v1/`:

| Endpoint | Method | ACP Operation | MockHub Mapping |
|---|---|---|---|
| `/acp/v1/checkout` | POST | Create Checkout | Clear cart → add items → checkout |
| `/acp/v1/checkout/{id}` | GET | Get Checkout | Get order by number |
| `/acp/v1/checkout/{id}` | PUT | Update Checkout | Cancel + recreate (PENDING only) |
| `/acp/v1/checkout/{id}/complete` | POST | Complete Checkout | Confirm order |
| `/acp/v1/checkout/{id}/cancel` | POST | Cancel Checkout | Fail order (releases tickets) |
| `/acp/v1/catalog` | GET | Product Catalog | Search events |
| `/acp/v1/listings` | GET | Offer Search | Search actionable ticket offers |

### Authentication

**MCP endpoints** use OAuth 2.1 with Dynamic Client Registration (DCR) when the `mcp-oauth2` profile is active. The embedded Spring Authorization Server handles token issuance and client registration. MCP clients (Claude, Cursor, etc.) connect by pointing at `https://mockhub.kousenit.com/mcp` — the OAuth flow is automatic. Without the `mcp-oauth2` profile, MCP falls back to `X-API-Key` header auth via `McpApiKeyFilter`.

**ACP endpoints** use API key authentication (`X-API-Key` header, configured via `mockhub.mcp.api-key`). The `AcpApiKeyFilter` handles this independently.

### ACP Checkout Flow

```
1. Agent discovers products and offers
   GET /acp/v1/catalog?query=jazz&city=NYC
   → Returns AcpCatalogItem[] with event-level discovery data
   GET /acp/v1/listings?query=yo-yo%20ma&city=New%20York&dateFrom=2026-04-01T00:00:00Z&dateTo=2026-04-30T23:59:59Z
   → Returns AcpListingItem[] with actionable listings sorted by price

2. Agent creates checkout
   POST /acp/v1/checkout
   {
     "buyerEmail": "buyer@example.com",
     "agentId": "shopping-agent-1",
     "mandateId": "abc-123",
     "lineItems": [{"listingId": 42, "quantity": 1}],
     "paymentMethod": "mock"
   }
   → Returns AcpCheckoutResponse with status CREATED

3. Agent completes checkout
   POST /acp/v1/checkout/MH-20260323-0001/complete
   X-Buyer-Email: buyer@example.com
   {
     "agentId": "shopping-agent-1",
     "mandateId": "abc-123"
   }
   → Returns AcpCheckoutResponse with status COMPLETED
```

### How ACP Maps to Existing Business Logic

The ACP layer is a pure **adapter** — it translates ACP's protocol vocabulary into MockHub's existing services:

```
ACP createCheckout  →  CartService.clearCart()
                    →  CartService.addToCart() × N
                    →  OrderService.checkout()

ACP completeCheckout →  PaymentService.createPaymentIntent() [mock or pre-existing intent]
                    →  PaymentService.confirmPayment()

ACP cancelCheckout   →  OrderService.failOrder()

ACP getCatalog      →  EventService.listEvents()
ACP getListings     →  EventService.listEvents() + ListingRepository.findByEventIdAndStatus()
```

No existing service was modified. The ACP controller and service wrap the same business logic that the MCP tools and REST API use.

### Connection to the Protocol Landscape

ACP is one of several emerging standards for agentic commerce:

| Protocol | Layer | Owner | MockHub Status |
|---|---|---|---|
| **ACP** | Checkout & merchant integration | Stripe + OpenAI | Implemented (Layer 3) |
| **AP2** | Trust & authorization | Google | Conceptually implemented via mandates (Layer 2) |
| **UCP** | Cross-vertical coordination | Google | Not implemented (orchestration layer) |
| **x402** | Machine-to-machine micropayments | Coinbase | Not implemented — interesting pattern, different problem ([details](#x402-http-402-for-machine-to-machine-payments)) |

## x402: HTTP 402 for Machine-to-Machine Payments

### What Is It?

[x402](https://www.x402.org/) is an open protocol that repurposes HTTP's long-dormant `402 Payment Required` status code for machine-to-machine API payments. The flow:

1. Client requests a protected endpoint (no auth needed)
2. Server responds `402 Payment Required` with pricing metadata
3. Client pays via USDC stablecoin (Base or Solana)
4. Client retries the request with a payment proof header
5. Server verifies payment and serves the response

Integration is middleware-based — sellers annotate routes with prices and wallet addresses, and the middleware handles the 402 handshake automatically. SDKs exist for Express, Next.js, FastAPI, Flask, and Go.

### Why MockHub Doesn't Implement It

x402 solves a different problem than MockHub. It monetizes **API access itself** (e.g., $0.001 per weather query). MockHub's agents are buying **products** (tickets) through the API — the API access is free, the tickets cost money.

| Concern | MockHub | x402 |
|---|---|---|
| What's being paid for | Concert tickets (products) | API calls (access) |
| Payment rails | Stripe (fiat currency) | USDC stablecoins (crypto) |
| Auth model | Mandates + OAuth2 | Crypto wallet signatures |
| Infrastructure | Standard web stack | Requires crypto wallet + facilitator service |

### The Interesting Idea vs. the Implementation

The *protocol pattern* — a server declaring "this endpoint costs $X, here's how to pay" in a machine-readable HTTP response — is genuinely useful. It's a clean, standards-based way for agents to discover and pay for API access without pre-registration or API keys.

However, the payment rails are a design choice, not a technical necessity. The same 402 handshake pattern could work with Stripe, PayPal, or any payment processor. The coupling to stablecoins adds wallet management, chain selection, and facilitator dependencies — infrastructure complexity that may or may not be justified depending on the use case.

### Teaching Takeaway

x402 is a good case study for students learning to evaluate emerging protocols:

- **Separate the pattern from the implementation.** HTTP 402 as a machine-readable payment signal is a good idea. Whether it requires crypto is a different question.
- **Ask who benefits from the coupling.** When a useful idea (API monetization) is bundled with an unrelated technology (blockchain), examine the incentive structure.
- **Consider the alternatives.** Stripe already handles micropayments. What does the crypto layer add that justifies the additional complexity?

For further reading: [x402 documentation](https://docs.x402.org/)

---

## Success Conditions

The implementation is considered working when all of the following are true:

1. An autonomous purchase cannot proceed without both `agentId` and a valid `mandateId`.
2. `findTickets` and `GET /acp/v1/listings` can answer a time-bounded query like "Yo-Yo Ma in New York next month" with actionable offers.
3. ACP and MCP purchase completion routes through `PaymentService`, not direct order confirmation.
4. Duplicate confirm/cancel/payment callbacks do not double-sell inventory, double-send notifications, or double-record mandate spend.
5. Successful confirmation increments mandate `totalSpent` exactly once.
6. `cd backend && ./gradlew test` passes.

---

## Configuration

### Properties

```yaml
# MCP OAuth2 (when mcp-oauth2 profile active)
mockhub.mcp.oauth2.issuer-uri: ${MCP_OAUTH2_ISSUER_URI:http://localhost:8080}

# ACP API key
mockhub.mcp.api-key: ${MCP_API_KEY:dev-api-key}

# Eval conditions
mockhub.eval.max-cart-total: 500          # SpendingLimitCondition threshold
mockhub.eval.price-plausibility.min: 0.1  # PricePlausibilityCondition bounds
mockhub.eval.price-plausibility.max: 10.0
```

### Endpoints Summary

```
MCP:    /mcp/**         — Spring AI MCP server (Streamable HTTP transport, OAuth2 auth)
ACP:    /acp/v1/**      — ACP-compatible REST endpoints (API key auth)
REST:   /api/v1/**      — Standard REST API (JWT auth)
OAuth2: /oauth2/**      — Authorization server (token, authorize, DCR, login)
```

### Connecting MCP Clients

Any MCP client that supports OAuth 2.1 can connect by pointing at the MCP URL:

| Client | Configuration |
|---|---|
| **Claude** (desktop/web/mobile) | Settings → Connectors → Add Custom Connector → URL: `https://mockhub.kousenit.com/mcp` |
| **Cursor** | Add to `mcp.json`: `"mockhub": { "url": "https://mockhub.kousenit.com/mcp" }` |
| **MCP Inspector** | URL: `https://mockhub.kousenit.com/mcp` (redirect URIs pre-registered) |

The OAuth2 discovery flow is automatic: client requests `/.well-known/oauth-protected-resource/mcp`, discovers the authorization server, registers via DCR, and obtains tokens.

---

## Teaching Connections

### For Students

1. **Layer 1 → Layer 2 progression**: Start by showing how MCP tools give agents capabilities, then show why unconstrained capabilities are dangerous (an agent buying $10,000 in tickets). Introduce mandates as the solution — formalized authorization that limits agent behavior.

2. **Eval conditions → Mandates → AP2**: MockHub's eval conditions are DbC-style preconditions. Mandates are a specialization of preconditions for agent authorization. AP2 is the industry standard version of the same concept. Students see the abstraction at three levels.

3. **MCP → ACP**: MCP tools are tightly coupled to Spring AI's tool-calling model. ACP is a protocol-level abstraction that any agent framework can use. Show how the same business logic serves both — the adapter pattern in practice.

4. **The trust problem**: When a human clicks "Buy Now," there's implicit authorization. When an agent calls `checkout()`, who authorized it? This is the fundamental problem of agentic commerce, and mandates are one solution.

### For Blog Posts / Videos

- "Making MockHub Agent-Friendly" — the journey from REST API to MCP tools to ACP endpoints
- "Design by Contract Meets Agent Authorization" — how eval conditions become mandates
- "The Agentic Commerce Stack" — ACP + AP2 + MCP explained with working code
- "Why Agents Need Permission Slips" — mandates as the missing piece of autonomous commerce

---

## Database Schema

### Mandates Table (V22)

```sql
CREATE TABLE mandates (
    id BIGSERIAL PRIMARY KEY,
    mandate_id VARCHAR(36) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    max_spend_per_transaction NUMERIC(12,2),
    max_spend_total NUMERIC(12,2),
    total_spent NUMERIC(12,2) NOT NULL DEFAULT 0,
    allowed_categories VARCHAR(1000),
    allowed_events VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);
```

---

## File Reference

### Phase 1: MCP Tools
- `backend/src/main/java/com/mockhub/mcp/tools/EventTools.java` — `findTickets`, `getListingDetail`
- `backend/src/main/java/com/mockhub/mcp/tools/OrderTools.java` — `confirmOrder`
- `backend/src/main/java/com/mockhub/eval/condition/SpendingLimitCondition.java`

### Phase 2: Mandates
- `backend/src/main/java/com/mockhub/mandate/` — entity, dto, repository, service
- `backend/src/main/java/com/mockhub/eval/condition/MandateCondition.java`
- `backend/src/main/java/com/mockhub/mcp/tools/MandateTools.java`
- `backend/src/main/resources/db/migration/V22__create_mandates_table.sql`

### Phase 3: ACP
- `backend/src/main/java/com/mockhub/acp/` — controller, service, dto, API key filter
- `docs/agentic-commerce.md` — this document

### MCP OAuth2 Authentication
- `backend/src/main/java/com/mockhub/mcp/config/McpOAuth2SecurityConfig.java` — authorization server + resource server chains
- `backend/src/main/java/com/mockhub/mcp/controller/McpOAuth2LoginController.java` — OAuth2 login and authorized pages
- `backend/src/main/resources/application-mcp-oauth2.yml` — profile config
