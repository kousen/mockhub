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
│  searchEvents, findTickets, getCommercePolicy │
└─────────────────────────────────────────────┘
```

**Layer 1** gives agents the tools to act. **Layer 2** constrains what they're allowed to do. **Layer 3** makes the system interoperable with other agent platforms.

---

## Layer 1: MCP Tools — The Agent's Capabilities

### Tool Inventory

MockHub exposes 34 MCP tools across 9 tool classes:

| Tool Class | Tools | Purpose |
|---|---|---|
| **EventTools** | `searchEvents`, `getEventDetail`, `getEventListings`, `getFeaturedEvents`, `getListingDetail`, `findTickets`, `compareTickets`, `getCommercePolicy` | Discovery, search, comparison, and purchase policy context |
| **PricingTools** | `getPriceHistory`, `getPricePrediction` | Price intelligence |
| **CartTools** | `getCart`, `addToCart`, `removeFromCart`, `clearCart`, `refreshCart` | Shopping cart management |
| **OrderTools** | `checkout`, `confirmOrder`, `getOrder`, `listOrders`, `getCalendarEntry` | Order lifecycle |
| **MandateTools** | `createMandate`, `revokeMandate`, `listMandates`, `validateMandate`, `getBestMandate` | Agent authorization |
| **AgentApprovalTools** | `proposePurchase`, `approvePurchase`, `denyPurchase`, `listPurchaseApprovals` | Purchase approval audit trail |
| **PaymentCredentialTools** | `issuePaymentCredential`, `listPaymentCredentials`, `revokePaymentCredential` | Scoped payment authority |
| **AgentRiskTools** | `getAgentRiskSummary` | Deterministic local risk and abuse visibility |
| **AgentPurchaseEvidenceTools** | `getAgentPurchaseEvidence` | Read-only purchase evidence trail for completed agent flows |

### The Complete Purchase Flow

An agent can now execute a full purchase on behalf of a user:

```
1. findTickets(query="concert", city="New York", dateFrom="2026-04-01T00:00:00Z",
               dateTo="2026-04-30T23:59:59Z", maxPrice=200)
   → Returns matching listings with event/date metadata and commercePolicyUrl, sorted by price

2. addToCart(userEmail="buyer@example.com", listingId=42,
             agentId="shopping-agent-1", mandateId="abc-123")
   → Eval conditions check: event in future, listing active
   → MandateCondition checks agent identity, mandate proof, and purchase authority
   → AgentRiskService records cart-hold activity and returns warnings when thresholds are crossed
   → Returns cart with any warnings

3. checkout(userEmail="buyer@example.com", paymentMethod="mock",
            agentId="shopping-agent-1", mandateId="abc-123")
   → Validates listings, reserves tickets, creates PENDING order
   → Requires paymentMethod to be exactly "mock" or "stripe"; use "mock" for the built-in mock-payment flow
   → AgentRiskCondition checks recent agent risk signals before the order is created
   → Records checkout attempts and returns OrderDto with any warnings

4. confirmOrder(userEmail="buyer@example.com", orderNumber="MH-20260323-0001",
                agentId="shopping-agent-1", mandateId="abc-123",
                paymentCredentialId="cred-789")
   → Routes through PaymentService
   → Validates and consumes the scoped payment credential when supplied
   → Records payment-credential validation failures as risk signals when they happen
   → Marks order CONFIRMED only after successful payment confirmation
   → Records mandate spend once, updates ticket status to SOLD, triggers SMS + email
   → Returns confirmed OrderDto with any risk warnings

5. getAgentPurchaseEvidence(userEmail="buyer@example.com",
                            orderNumber="MH-20260323-0001")
   → Returns the read-only mandate, approval, payment credential, checkout, risk,
     eval, actor timeline, and fulfillment evidence trail for the order owner
```

### Purchase Approval Records

Mandates answer whether an agent is authorized to act. Approval records answer who saw a proposed purchase, what commercial context they saw, and what happened next.

Agents can create a proposal with `proposePurchase`, including a snapshot of the proposed listings or order, the agent rationale, price and fee totals, and the relevant commerce policy snapshot. The user can then approve or deny that proposal. An approved `approvalId` may be supplied to `confirmOrder` or ACP `completeCheckout` to link the final purchase to the recorded approval.

Approval IDs are optional for `AUTO_PURCHASE` mandates. Mandates with `approvalMode=APPROVAL_REQUIRED`
require an approved purchase approval before MCP `confirmOrder` or ACP `completeCheckout` can finish
the purchase.

### Scoped Payment Credentials

Mandates answer **may this agent act?** Scoped payment credentials answer **may this agent pay with this instrument under these limits?** MockHub models that second question separately in `com.mockhub.paymentcredential`.

The initial implementation is deliberately mock-backed:

- `issuePaymentCredential` creates a credential for a user, agent, maximum amount, currency, usage type, backing payment method, and optional expiration.
- `confirmOrder` and ACP `completeCheckout` accept an optional `paymentCredentialId`.
- If supplied, the credential is validated before payment confirmation against user, agent, merchant (`MOCKHUB`), currency, payment method, expiration, status, and amount limit.
- One-time credentials are consumed exactly once for the order number. Repeated completion for the same confirmed order is idempotent; reuse for another order is rejected.
- Revoked, expired, over-limit, wrong-user, or wrong-agent credentials fail before money moves.
- MCP issuance mirrors `MandateTools` and is constrained by the same identity binding (see [Who is the user?](#who-is-the-user-identity-binding) below): under the `mcp-oauth2` profile the credential is issued for the OAuth-authenticated user regardless of the `userEmail` parameter; under `X-API-Key` auth the email is self-asserted.

This keeps three commercial facts separate for students:

| Question | MockHub model |
|---|---|
| Can the agent take this action? | Mandate + `MandateCondition` |
| Did a human approve this specific proposal? | Purchase approval record |
| Can the agent pay with this payment authority? | Scoped payment credential |

### Agent Risk and Abuse Signals

Useful shopping agents and abusive automation arrive through the same doors: cart holds, mandate proofs, checkout attempts, and payment credentials. MockHub now records deterministic local risk signals for those agent actions without introducing an external fraud provider.

The initial model lives in `com.mockhub.agentrisk` and records:

- Cart-hold attempts and rapid cart-hold bursts
- Checkout and confirmation attempts
- Mandate mismatches
- Failed checkouts
- High-spend attempts over `mockhub.agent-risk.high-spend-threshold`
- Payment-credential validation failures

`AgentRiskCondition` reads the recent signal window during MCP and ACP purchase flows. Repeated mandate mismatches become a CRITICAL eval failure and block the action. Repeated failed checkouts, rapid cart holds, and high-spend attempts remain WARNING-level signals that are returned to MCP agents alongside the normal cart or order payload. Agents can also call `getAgentRiskSummary(userEmail, agentId)` to inspect the recent signals, warning reasons, highest severity, and blocked status.

This is deliberately smaller than a production fraud stack. The teaching point is the boundary: authorization says whether the agent is allowed to act, payment credentials say whether it can pay, and risk signals describe whether the pattern of behavior still looks acceptable.

### Agent Purchase Evidence Trails

Agentic checkout adds more proof obligations than traditional checkout. A normal MockHub order needs buyer, cart, payment, ticket, and notification evidence. An agent order also needs to show which agent acted, what mandate authorized it, whether a user approval record was required and completed, which scoped payment credential paid, what risk signals were recorded, and which eval conditions passed, warned, or blocked.

MockHub exposes that assembled record through:

- `GET /api/v1/orders/{orderNumber}/agent-evidence` for the authenticated order owner or an admin.
- MCP `getAgentPurchaseEvidence(userEmail, orderNumber)` for agent self-inspection. Website chat calls still use `ChatContext` so the authenticated user's email overrides any LLM-supplied email.

The evidence service is intentionally read-only. It assembles existing durable records from `Order`, `Mandate`, `AgentPurchaseApproval`, `PaymentCredential`, and `AgentRiskSignal`, then derives ACP-aligned checkout status and fulfillment artifact links. Missing optional pieces are returned as `null` or empty lists rather than errors, because some legitimate orders use `AUTO_PURCHASE`, skip scoped credentials, or predate risk-signal persistence.

| Evidence area | Traditional checkout | Agent checkout evidence |
|---|---|---|
| Authority | Authenticated buyer owns the cart/order | Buyer owner plus `agentId`, mandate scope, budget, expiry, and approval mode |
| Human approval | Not separately modeled | Optional/required `AgentPurchaseApproval` record with proposal, decision, and final order |
| Payment | Payment method and payment intent | Payment method plus optional scoped credential status and consumption state |
| Risk/eval | Standard validation failures | Persisted local risk signals plus derived eval outcomes such as mandate authorization, purchase approval, payment credential, and agent risk |
| Fulfillment | PDF tickets, QR verification, email/SMS notification attempts | Same fulfillment artifacts, attached to an actor timeline for the agent purchase flow |

This fills one of the UCP-readiness gaps noted below: MockHub still does not publish a UCP profile or versioned UCP service envelope, but it can now explain the evidence behind an agent checkout in a single owner-scoped record.

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
    maxResults: 5,           // limit results
    userEmail: "buyer@example.com" // optional: apply stored buyer preferences
)
```

This is an example of **agent-ergonomic API design** — reducing round-trips and letting agents express intent in a single call. Traditional REST APIs are designed for human-driven UIs with navigation; agent APIs should support goal-directed actions.

When `userEmail` is supplied, `findTickets` may fill missing city, category, section, or maximum-price filters from the user's explicit buyer preferences. The response then includes `preferenceContext` metadata so the agent can tell the user which stored preferences affected the search.

### Agent-Friendly Ticket Comparison

Raw search results are not enough when an agent has to explain a purchase recommendation. MockHub exposes a deterministic `compareTickets` MCP tool for agent shopping decisions:

```
compareTickets(
    query: "jazz",
    city: "New York",
    maxPrice: 150,
    preferredSection: "Orchestra",
    maxResults: 10,
    userEmail: "buyer@example.com"
)
```

The response includes:

- ranked options with objective listing data nested separately from heuristic judgment fields
- cheapest, best-value, best-section, and lowest-risk recommendations
- reason codes such as `COMPETITIVE_PRICE`, `MATCHES_REQUESTED_SECTION`, and `LOWER_MARKETPLACE_RISK`
- deterministic rationale text suitable for explaining the tradeoff to a user
- price plausibility warnings based on the returned market set, including high-price anomalies, unusually low outliers, and limited comparison depth
- optional `preferenceContext` metadata when stored buyer preferences fill missing search filters or ranking hints

The comparison service intentionally starts with deterministic heuristics rather than an LLM. That keeps the teaching story transparent: students can inspect exactly how price, section quality, seller risk, and warning penalties affect the ranking. If AI-generated prose is added later, it should remain clearly labeled and fall back to these deterministic fields.

### Buyer Preference Memory

Buyer preference memory gives agents a structured way to translate fuzzy intent into a purchasing brief without treating inferred data as the whole truth. Authenticated users can store explicit ticket-shopping preferences through `GET/PUT /api/v1/preferences/me`.

The preference model tracks preferred artists, categories, cities, venues, and sections; disliked venues and categories; maximum all-in price; service-fee tolerance; all-in price preference; accessibility needs; risk tolerance; and willingness to wait for price drops. Spotify listening data, favorites, and purchase history remain separate signals.

MockHub uses preferences in three visible places:

- `findTickets` applies missing search filters when `userEmail` is supplied.
- `compareTickets` applies missing filters and a preferred section ranking hint when `userEmail` is supplied.
- AI recommendations include explicit preferences in the prompt and return `preferenceContext` metadata.

The preference metadata is deliberately explicit. It tells agents which preferences were applied, while avoiding raw Spotify listening details in agent-facing preference context.

### Agent-Readable Commerce Policies

Agents need purchase policy context before they commit a user to checkout. MockHub exposes structured commerce policies through both REST and agent-facing responses:

| Surface | Policy Access |
|---|---|
| REST | `GET /api/v1/commerce/policies/default`, `GET /api/v1/commerce/policies/events/{eventSlug}` |
| MCP | `getCommercePolicy(eventSlug)` returns the full policy; `getEventListings` embeds it; `findTickets` and `getListingDetail` include `commercePolicyUrl` |
| ACP | Catalog items, listing items, and checkout responses include `commercePolicy` |

The policy document is intentionally structured rather than prose-only. It includes a stable `policyId`, `version`, event scope, `policyUrl`, `updatedAt`, policy sections (`refunds`, `cancellations`, `ticket_transfer`, `fees`, `support_and_disputes`), and support contact fields. This lets agents answer questions like "can this be refunded?" or "what fees apply?" without scraping UI text.

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
- **Section restrictions**: Only buy tickets in specific venue sections (e.g., "Floor,Lower Bowl").
- **Approval mode**: `AUTO_PURCHASE` allows direct completion within the mandate; `APPROVAL_REQUIRED` requires an approved purchase approval before completion.
- **Expiration**: Optional time-bound authorization.

### Mandate Lifecycle

```
1. User grants mandate to agent
   createMandate(agentId="shopping-agent-1", userEmail="buyer@example.com",
                 scope="PURCHASE", maxSpendPerTransaction=200, maxSpendTotal=1000,
                 allowedCategories="jazz,rock", allowedSections="Floor,Lower Bowl",
                 approvalMode="APPROVAL_REQUIRED")

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
6. Check category, event, and section restrictions. A section-restricted mandate only authorizes a
   purchase when the listing or cart item supplies a matching section name.
7. For mandates with `approvalMode=APPROVAL_REQUIRED`, require an approved purchase approval before order completion

If any check fails, the condition returns a CRITICAL failure — the action is blocked.

### Connection to AP2

Google's Agent Payments Protocol (AP2) defines "mandates" as digitally signed statements of agent authority. MockHub's mandates serve the same purpose but are enforced through the eval conditions framework rather than cryptographic signatures.

The conceptual mapping:

| AP2 Concept | MockHub Implementation |
|---|---|
| Digitally signed mandate | Database record checked by `MandateCondition` |
| Scope (capabilities) | `scope` field (BROWSE/PURCHASE) |
| Spending limits | `maxSpendPerTransaction`, `maxSpendTotal` |
| Conditional restrictions | `allowedCategories`, `allowedEvents`, `allowedSections` |
| Approval behavior | `approvalMode` field (AUTO_PURCHASE/APPROVAL_REQUIRED) |
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

The Agentic Commerce Protocol (ACP) is an open standard codeveloped by Stripe, OpenAI, and Meta that enables programmatic commerce between AI agents and businesses. It defines a RESTful interface for checkout operations that any ACP-compatible agent can use.

### MockHub's ACP Implementation

MockHub exposes ACP-shaped adapter endpoints at `/acp/v1/`:

| Endpoint | Method | ACP Operation | MockHub Mapping |
|---|---|---|---|
| `/acp/v1/checkout` | POST | Create Checkout | Clear cart → add items → checkout |
| `/acp/v1/checkout/{id}` | GET | Get Checkout | Get order by number |
| `/acp/v1/checkout/{id}` | PUT | Update Checkout | Cancel + recreate (PENDING only) |
| `/acp/v1/checkout/{id}/complete` | POST | Complete Checkout | Confirm order |
| `/acp/v1/checkout/{id}/cancel` | POST | Cancel Checkout | Fail order (releases tickets) |
| `/acp/v1/catalog` | GET | Product Catalog | Search events |
| `/acp/v1/listings` | GET | Offer Search | Search actionable ticket offers |

The listing search endpoint is a MockHub offer-discovery extension around the pinned ACP snapshot, not a full ACP conformance claim.

### Authentication

**MCP endpoints** use OAuth 2.1 with Dynamic Client Registration (DCR) when the `mcp-oauth2` profile is active. The embedded Spring Authorization Server handles token issuance and client registration. MCP clients (Codex, Claude, Cursor, etc.) connect directly to `https://mockhub.kousenit.com/mcp` — the OAuth flow is automatic. Production uses this OAuth setup; do not configure `mcp-remote` or `X-API-Key` headers for MCP client access.

**ACP endpoints** use API key authentication (`X-API-Key` header, configured via `mockhub.mcp.api-key`). The `AcpApiKeyFilter` handles this independently.

### Who is the user? (identity binding)

Every user-scoped tool takes a `userEmail` parameter. The trust question is: **who decides that value?** Authentication (proving identity) and authorization (granting an agent authority) are separate acts that happen at separate times — agentic commerce removes the shopping UI, not the one-time identity step.

- **`mcp-oauth2` profile (production / native Claude connector).** The user authorizes the connector once via the OAuth 2.1 authorization-code login (MockHub's own `/oauth2/login`, backed by `UserDetailsServiceImpl`). The access token's subject is that user's email. `McpAuthenticatedEmailFilter` runs on the `/mcp/**` resource-server chain, resolves the token subject to a known user, and pins it into `ChatContext` for the request. Every tool then calls `ChatContext.resolveEmail(...)`, which **ignores** the `userEmail` parameter and uses the authenticated identity. Consequence: an agent can search, create a mandate, issue a payment credential, and purchase entirely through MCP — but only ever *as the user who logged in*. It cannot act as, or mint authority for, anyone else. This is what makes the "delegate the whole purchase to your agent, never visit the website" demo trustworthy rather than forgeable.
- **`X-API-Key` fallback (local / dev / trusted operator).** There is no per-user identity — the key authenticates the *deployment*, not a person — so `userEmail` is self-asserted and honored as-is. Use the `mcp-oauth2` profile whenever per-user identity binding matters.
- **Website chat** uses the same `ChatContext` mechanism: `ChatService` pins the logged-in user before invoking the ChatClient tools.

Note that `agentId` remains self-asserted in all modes (agents identify themselves). This is safe because a mandate lookup matches on *both* `agentId` and the resolved `userEmail`, so an agent cannot borrow another agent's mandate for a user.

### ACP Checkout Flow

```
1. Agent discovers products and offers
   GET /acp/v1/catalog?query=jazz&city=NYC
   → Returns AcpCatalogItem[] with event-level discovery data and commercePolicy
   GET /acp/v1/listings?query=yo-yo%20ma&city=New%20York&dateFrom=2026-04-01T00:00:00Z&dateTo=2026-04-30T23:59:59Z
   → Returns AcpListingItem[] with actionable listings sorted by price and commercePolicy

2. Agent creates checkout
   POST /acp/v1/checkout
   {
     "buyerEmail": "buyer@example.com",
     "agentId": "shopping-agent-1",
     "mandateId": "abc-123",
     "lineItems": [{"listingId": 42, "quantity": 1}],
     "paymentMethod": "mock"
   }
   → Returns AcpCheckoutResponse with status CREATED and commercePolicy
   → paymentMethod is optional for ACP checkout; when present it must be exactly "mock" or "stripe"

3. Agent completes checkout
   POST /acp/v1/checkout/MH-20260323-0001/complete
   X-Buyer-Email: buyer@example.com
   {
     "agentId": "shopping-agent-1",
     "mandateId": "abc-123",
     "paymentCredentialId": "cred-789"
   }
   → Returns AcpCheckoutResponse with status COMPLETED and commercePolicy
```

### How ACP Maps to Existing Business Logic

The ACP layer is a pure **adapter** — it translates ACP's protocol vocabulary into MockHub's existing services:

```
ACP createCheckout  →  CartService.clearCart()
                    →  CartService.addToCart() × N
                    →  OrderService.checkout()

ACP completeCheckout →  PaymentCredentialService.authorizeForPayment() [optional]
                    →  PaymentService.createPaymentIntent() [mock or pre-existing intent]
                    →  PaymentCredentialService.consumeForPayment() [optional]
                    →  PaymentService.confirmPayment()

ACP cancelCheckout   →  OrderService.failOrder()

ACP getCatalog      →  EventService.listEvents()
ACP getListings     →  EventService.listEvents() + ListingRepository.findByEventIdAndStatus()
Policy context      →  CommercePolicyService
```

The ACP controller and service wrap the same business logic that the MCP tools and REST API use. The commerce policy service is deliberately read-only and static for now, so policy context can be added to agent responses without changing checkout behavior.

### Connection to the Protocol Landscape

ACP is one of several emerging standards for agentic commerce:

| Protocol | Layer | Owner | MockHub Status |
|---|---|---|---|
| **ACP** | Checkout, cart, payment delegation, merchant integration | Stripe + OpenAI + Meta | Partially implemented through `/acp/v1/**` (Layer 3) |
| **AP2** | Trust, authorization, signed mandates, evidence | Google | Conceptually implemented through mandates and approval records (Layer 2) |
| **UCP** | Cross-surface commerce discovery and capability negotiation | Google + industry partners | Not UCP-compliant; many concepts map to existing MockHub surfaces |
| **x402** | Machine-to-machine API payments over HTTP 402 | [x402.org](https://docs.x402.org/) + Coinbase CDP | Not implemented — interesting pattern, different problem ([details](#x402-http-402-for-machine-to-machine-payments)) |

[ACP](https://www.agenticcommerce.dev/) is the closest match to MockHub's current protocol implementation: it gives agents a checkout API that wraps existing cart, order, and payment logic. [AP2](https://ap2-protocol.org/ap2/specification/) is the closest match to MockHub's authorization model: its checkout and payment mandates line up with the same trust problem MockHub handles through database-backed mandates, approval records, and eval conditions.

[UCP](https://ucp.dev/2026-04-08/specification/overview/) (spec snapshot dated 2026-04-08) sits one layer wider. Its official docs describe a common language for platforms, agents, and businesses across discovery, checkout, identity linking, payment handling, and post-purchase experiences. UCP profiles are discovered at `/.well-known/ucp`, advertise services and capabilities, and negotiate protocol and capability versions. [Google's UCP guide](https://developers.google.com/merchant/ucp/) emphasizes that merchants remain the merchant of record, keep their customer relationship, and participate in a full end-to-end shopping journey rather than checkout alone.

### Spec Versions

These pins are documentation targets, not conformance claims. They were last verified on **2026-05-15** against the linked public specs and guides. ACP's public docs link to the GitHub repository used for versioned spec snapshots. x402's public docs describe x402.org as the neutral documentation source, link to the foundation repository, and point to Coinbase CDP docs during the v2 migration.

| Protocol | Target revision | Canonical reference | Last verified | MockHub stance |
|---|---|---|---|---|
| **ACP** | `2026-04-17` spec snapshot | [ACP spec `2026-04-17`](https://github.com/agentic-commerce-protocol/agentic-commerce-protocol/tree/main/spec/2026-04-17) | 2026-05-15 | MockHub implements ACP-shaped checkout and catalog adapters at `/acp/v1/**`, but has not claimed full ACP conformance. |
| **AP2** | AP2 `v0.2` | [AP2 specification heading](https://ap2-protocol.org/ap2/specification/#agentic-payment-protocol-v02) | 2026-05-15 | MockHub maps the authorization and evidence concepts through mandates, approval records, and eval conditions; it does not issue cryptographically signed AP2 mandate credentials. |
| **UCP** | `2026-04-08` protocol version | [UCP specification overview](https://ucp.dev/2026-04-08/specification/overview/) and [Google UCP guide](https://developers.google.com/merchant/ucp/) | 2026-05-15 | MockHub tracks readiness only: no UCP profile, UCP service envelope, or UCP version negotiation is published yet. |
| **x402** | Protocol `v2` | [x402 v2 specification](https://github.com/x402-foundation/x402/blob/main/specs/x402-specification-v2.md) and [migration guide](https://docs.cdp.coinbase.com/x402/migration-guide) | 2026-05-15 | MockHub intentionally does not implement x402 because tickets are the paid product; API access itself is not monetized. |

Verification means the links resolved successfully and the visible version markers matched the pinned target (`spec/2026-04-17`, AP2 heading `v0.2`, UCP `ucp.version` `2026-04-08`, and x402 `Protocol Version: 2`). Re-check this table before release prep, before opening a future UCP profile implementation issue, and whenever MockHub changes its ACP, MCP, or agent discovery surfaces. There is no automated spec-freshness check yet.

MockHub does **not** claim UCP compliance today. The honest position is UCP readiness: MockHub already has concrete surfaces that map to UCP concepts, and it has a clear backlog for the missing pieces.

### UCP Readiness Mapping

Coverage labels in this table describe MockHub surfaces that exist today. They do not mean the surface is UCP-conformant. The references for this mapping are pinned in [Spec Versions](#spec-versions).

| UCP Concept | MockHub Surface | MockHub Coverage | Notes |
|---|---|---|---|
| Business profile and capability discovery | `llms.txt`, MCP OAuth metadata, A2A agent card | Not implemented | MockHub exposes agent-readable metadata, but not a UCP business profile or capability negotiation document. Issue #237 concluded that publishing `/.well-known/ucp` now would overstate UCP support. |
| Catalog and offer discovery | MCP `searchEvents`, `findTickets`, `compareTickets`; ACP `GET /catalog` and `GET /listings`; REST event/listing APIs | Implemented | Agents can discover products and actionable ticket offers through both MCP and ACP-shaped endpoints. |
| Cart lifecycle | MCP cart tools, REST cart API, ACP checkout creation/update flow | Implemented | ACP treats checkout creation as the agent-facing cart/checkout boundary; MCP exposes explicit cart operations. |
| Checkout lifecycle | ACP `/acp/v1/checkout/**`, MCP `checkout` and `confirmOrder`, `OrderService`, `PaymentService` | Partial | MockHub supports create, update, cancel, and complete flows, but does not implement UCP version negotiation or UCP response envelopes. |
| Order and post-purchase support | MCP `getOrder`, `listOrders`, `getCalendarEntry`, `getAgentPurchaseEvidence`; REST agent evidence endpoint; public ticket view; PDF/QR tickets; SMS/email fulfillment | Partial | MockHub covers order lookup, fulfillment, and owner-scoped agent evidence well for tickets, though it does not yet model returns, disputes, or shipment tracking. |
| Identity linking | Website OAuth account linking, authenticated website user context, MCP OAuth2 for agent access | Partial | MockHub has account linking and authenticated agent transport, but not UCP identity-linking capability declarations. Buyer preference memory adds explicit user context without becoming a UCP identity-linking surface. |
| Commerce policy | REST commerce policy endpoints, MCP `getCommercePolicy`, policy snapshots in ACP/listing responses | Implemented | Agents can fetch structured policy context before proposing or completing purchases. |
| Authorization and proof | Mandates, `MandateCondition`, approval records, approval-required mandates, agent purchase evidence trails | Partial | These map closely to AP2 concepts, but they are not cryptographically signed AP2 mandate credentials. Scoped payment credentials now keep "allowed to act" separate from "allowed to pay"; evidence trails assemble the records after checkout. |
| Payment credentials | `PaymentCredentialService`, `PaymentCredentialTools`, ACP/MCP `paymentCredentialId`, mock payment service | Partial | MockHub supports mock-backed scoped credentials with one-time/reusable usage, expiration, revocation, limits, and checkout validation. Stripe-backed wallet credentials remain future work. |
| Risk and abuse signals | `AgentRiskSignal`, `AgentRiskService`, `AgentRiskCondition`, MCP `getAgentRiskSummary` | Partial | MockHub persists deterministic local risk signals, returns warnings to MCP agents, and blocks repeated mandate mismatches. It does not integrate an external fraud/risk provider. |
| Preference and personalization memory | `BuyerPreferenceService`, `buyer_preferences`, `GET/PUT /api/v1/preferences/me`, MCP `findTickets`/`compareTickets`, AI recommendations | Partial | MockHub stores explicit buyer preferences and returns preference-use metadata. It does not publish UCP capability declarations or cross-platform preference synchronization. |
| Extensions and version negotiation | None | Not implemented | UCP's capability model relies on dated protocol versions and negotiated capability versions. MockHub should not advertise support until it can produce a truthful profile and validate versioned requests. |
| Machine-to-machine API payments | None | Intentionally out of scope | x402 and similar rails monetize API access. MockHub's agents buy tickets; API access itself is free. |

### UCP Discovery Profile Decision

[Issue #237](https://github.com/kousen/mockhub/issues/237) asked whether MockHub should publish a minimal `/.well-known/ucp` business profile now. The recommendation is: **do not publish a UCP profile yet**.

The reason is not that MockHub lacks agentic-commerce capabilities. It has plenty of them. The problem is that a UCP profile is not just a static metadata file; it is the protocol discovery document for versioned UCP services, capabilities, payment handlers, and negotiation behavior. Publishing a placeholder profile would invite platforms to treat MockHub as UCP-capable when the advertised endpoints still speak MockHub's existing ACP, MCP, and REST contracts.

MockHub should wait until it can do at least the following:

1. Keep the pinned UCP protocol version in [Spec Versions](#spec-versions) current, including the relevant service/capability schema references.
2. Expose UCP-shaped service bindings or clearly documented adapters, rather than pointing UCP clients at ACP endpoints with different request and response envelopes.
3. Keep payment credentials separate from mandates (#218), because UCP payment handlers and AP2-style autonomous payment flows depend on that boundary.
4. Preserve basic agent risk signals (#217), because UCP's signal model expects transaction-environment data for authorization, rate limiting, and abuse prevention.
5. Decide whether buyer-preference state belongs in the first profile or remains outside the UCP surface.
6. Add tests that assert the profile content matches the code's live endpoints and supported capabilities.

Until then, `llms.txt`, MCP OAuth metadata, ACP endpoints, and the A2A agent card remain the honest discovery surfaces. No follow-up UCP profile implementation issue exists yet; open one only after the UCP service/profile tests are scoped. Use [Spec Versions](#spec-versions) as the pinned protocol reference, and include profile-content tests in that future issue.

### Why UCP Matters for the Training Course

UCP is useful in the August 2026 course because it forces the broader question: can an agent complete a whole commerce journey, not just call a checkout endpoint? MockHub is a good teaching system precisely because the pieces are visible:

1. Discovery happens through MCP tools, ACP catalog/listing routes, and `llms.txt`.
2. Authorization happens through mandates and approval records.
3. Payment still routes through ordinary payment services, with scoped payment credentials making agent payment authority explicit before confirmation.
4. Fulfillment happens through tickets, QR codes, calendar files, and notifications.
5. Risk is explicit through local signals, and preference memory is explicit user data rather than invisible inference.

That lets students compare a working partial system against the protocol map. The lesson is not "MockHub implements every new standard." The lesson is that responsible agentic commerce needs a legible division of responsibility: capability discovery, user authority, payment authority, risk evaluation, checkout, fulfillment, and evidence.

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
2. `findTickets`, `compareTickets`, and `GET /acp/v1/listings` can answer a time-bounded query like "find me a concert in New York next month under $200" with actionable offers and explainable tradeoffs.
3. ACP and MCP purchase completion routes through `PaymentService`, not direct order confirmation.
4. Duplicate confirm/cancel/payment callbacks do not double-sell inventory, double-send notifications, or double-record mandate spend.
5. Successful confirmation increments mandate `totalSpent` exactly once.
6. Repeated mandate mismatches for the same user and agent create persisted risk signals and cause `AgentRiskCondition` to block later purchase actions.
7. `getAgentRiskSummary` returns recent signals, warning reasons, highest severity, and blocked status for a user/agent pair.
8. `getAgentPurchaseEvidence` and `GET /api/v1/orders/{orderNumber}/agent-evidence` return the assembled agent purchase evidence record for the order owner or an admin.
9. Users can store and update buyer preferences, and preference-aware searches return explicit `preferenceContext` metadata.
10. `cd backend && ./gradlew test` passes.
11. UCP readiness documentation is manually checked against the current MockHub tool/endpoint names and the pinned external protocol references. There is no automated UCP conformance check yet.
12. The [Spec Versions](#spec-versions) table is re-checked before release prep and before any future UCP profile work. There is no automated spec-freshness check yet.

---

## Configuration

### Properties

```yaml
# MCP OAuth2 (when mcp-oauth2 profile active)
mockhub.mcp.oauth2.issuer-uri: ${MCP_OAUTH2_ISSUER_URI:http://localhost:8080}
# Access tokens refresh silently; TTL only bounds a stolen bearer token's usefulness.
mockhub.mcp.oauth2.access-token-ttl: ${MCP_OAUTH2_ACCESS_TOKEN_TTL:8h}
# Refresh tokens rotate, so this is a sliding window: active connectors never
# re-auth, idle ones expire. Spending is bounded by mandates, not token lifetime.
mockhub.mcp.oauth2.refresh-token-ttl: ${MCP_OAUTH2_REFRESH_TOKEN_TTL:60d}

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
| **Codex** | `codex mcp add mockhub --url https://mockhub.kousenit.com/mcp` |
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

### Mandates Table (V22 + V32 conditional fields)

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
    allowed_sections VARCHAR(1000),
    approval_mode VARCHAR(30) NOT NULL DEFAULT 'AUTO_PURCHASE',
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
- `backend/src/main/java/com/mockhub/mcp/tools/EventTools.java` — `findTickets`, `compareTickets`, `getListingDetail`, `getCommercePolicy`
- `backend/src/main/java/com/mockhub/ticket/service/TicketComparisonService.java` — deterministic ticket ranking and price warning heuristics
- `backend/src/main/java/com/mockhub/mcp/tools/OrderTools.java` — `confirmOrder`
- `backend/src/main/java/com/mockhub/eval/condition/SpendingLimitCondition.java`

### Phase 2: Mandates
- `backend/src/main/java/com/mockhub/mandate/` — entity, dto, repository, service
- `backend/src/main/java/com/mockhub/eval/condition/MandateCondition.java`
- `backend/src/main/java/com/mockhub/mcp/tools/MandateTools.java`
- `backend/src/main/resources/db/migration/V22__create_mandates_table.sql`

### Phase 2b: Purchase Approval Records
- `backend/src/main/java/com/mockhub/agentapproval/` — approval audit entity, DTOs, service, controller
- `backend/src/main/java/com/mockhub/mcp/tools/AgentApprovalTools.java`
- `backend/src/main/resources/db/migration/V31__create_agent_purchase_approvals.sql`

### Phase 2c: Agent Purchase Evidence
- `backend/src/main/java/com/mockhub/agentpurchaseevidence/` — owner/admin-scoped evidence DTOs, service, and REST controller
- `backend/src/main/java/com/mockhub/mcp/tools/AgentPurchaseEvidenceTools.java` — MCP self-inspection tool
- `backend/src/test/java/com/mockhub/AgentPurchaseEvidenceIntegrationTest.java` — full mandate, approval, credential, checkout, and fulfillment evidence flow

### Phase 3: ACP
- `backend/src/main/java/com/mockhub/acp/` — controller, service, dto, API key filter
- `docs/agentic-commerce.md` — this document

### Commerce Policies
- `backend/src/main/java/com/mockhub/commerce/` — agent-readable commerce policy controller, service, and DTOs

### Buyer Preferences
- `backend/src/main/java/com/mockhub/buyerpreference/` — explicit buyer preference memory, authenticated API, and preference application service
- `backend/src/main/resources/db/migration/V35__create_buyer_preferences.sql`

### MCP OAuth2 Authentication
- `backend/src/main/java/com/mockhub/mcp/config/McpOAuth2SecurityConfig.java` — authorization server + resource server chains
- `backend/src/main/java/com/mockhub/mcp/controller/McpOAuth2LoginController.java` — OAuth2 login and authorized pages
- `backend/src/main/resources/application-mcp-oauth2.yml` — profile config
