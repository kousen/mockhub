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

MockHub exposes 22 MCP tools across 5 tool classes:

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
1. findTickets(query="Taylor Swift", city="NYC", maxPrice=200)
   → Returns matching listings across events, sorted by price

2. addToCart(userEmail="buyer@example.com", listingId=42)
   → Eval conditions check: event in future, listing active
   → Spending limit warning if cart exceeds $2,000
   → Returns cart with any warnings

3. checkout(userEmail="buyer@example.com", paymentMethod="mock")
   → Validates listings, reserves tickets, creates PENDING order
   → Returns OrderDto with order number

4. confirmOrder(userEmail="buyer@example.com", orderNumber="MH-20260323-0001")
   → Marks order CONFIRMED, updates ticket status to SOLD
   → Triggers SMS + email notifications
   → Returns confirmed OrderDto
```

### Key Design: `findTickets` — The Compound Search Tool

Before `findTickets`, agents needed three round-trips to find a ticket (search events → get detail → get listings). `findTickets` collapses this into a single call with rich filtering:

```
findTickets(
    query: "jazz",           // text search on event name/artist
    category: "jazz",        // category filter
    city: "New York",        // venue city
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

Without mandates, any agent with the MCP API key can buy anything for anyone. That's fine for demos but unacceptable for real commerce. The fundamental question: **when an autonomous agent initiates a purchase, how do we verify the user authorized it?**

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

1. Look up active, non-expired mandate for the agent+user pair
2. Check the scope matches the action (BROWSE for reads, PURCHASE for buys)
3. Check per-transaction spending limit
4. Check cumulative spending limit (tracks `totalSpent`)
5. Check category and event restrictions

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

### Authentication

ACP endpoints use the same API key authentication as MCP (`X-API-Key` header, configured via `mockhub.mcp.api-key`). The `AcpApiKeyFilter` handles this independently of the MCP filter.

### ACP Checkout Flow

```
1. Agent discovers products
   GET /acp/v1/catalog?query=jazz&city=NYC
   → Returns AcpCatalogItem[] with event details and pricing

2. Agent creates checkout
   POST /acp/v1/checkout
   {
     "buyerEmail": "buyer@example.com",
     "lineItems": [{"listingId": 42, "quantity": 1}],
     "paymentMethod": "mock"
   }
   → Returns AcpCheckoutResponse with status CREATED

3. Agent completes checkout
   POST /acp/v1/checkout/MH-20260323-0001/complete
   X-Buyer-Email: buyer@example.com
   → Returns AcpCheckoutResponse with status COMPLETED
```

### How ACP Maps to Existing Business Logic

The ACP layer is a pure **adapter** — it translates ACP's protocol vocabulary into MockHub's existing services:

```
ACP createCheckout  →  CartService.clearCart()
                    →  CartService.addToCart() × N
                    →  OrderService.checkout()

ACP completeCheckout →  OrderService.confirmOrder()

ACP cancelCheckout   →  OrderService.failOrder()

ACP getCatalog      →  EventService.listEvents()
```

No existing service was modified. The ACP controller and service wrap the same business logic that the MCP tools and REST API use.

### Connection to the Protocol Landscape

ACP is one of several emerging standards for agentic commerce:

| Protocol | Layer | Owner | MockHub Status |
|---|---|---|---|
| **ACP** | Checkout & merchant integration | Stripe + OpenAI | Implemented (Layer 3) |
| **AP2** | Trust & authorization | Google | Conceptually implemented via mandates (Layer 2) |
| **UCP** | Cross-vertical coordination | Google | Not implemented (orchestration layer) |
| **x402** | Machine-to-machine micropayments | Coinbase | Not applicable (ticket sales, not API micropayments) |

---

## Configuration

### Properties

```yaml
# MCP / ACP API key (shared)
mockhub.mcp.api-key: ${MCP_API_KEY:dev-api-key}

# Eval conditions
mockhub.eval.max-cart-total: 500          # SpendingLimitCondition threshold
mockhub.eval.price-plausibility.min: 0.1  # PricePlausibilityCondition bounds
mockhub.eval.price-plausibility.max: 10.0
```

### Endpoints Summary

```
MCP:  /mcp/**         — Spring AI MCP server (SSE transport)
ACP:  /acp/v1/**       — ACP-compatible REST endpoints
REST: /api/v1/**       — Standard REST API (frontend)
```

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
