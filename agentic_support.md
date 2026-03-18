# Making Your REST API Agent-Friendly

Great timing — this is exactly the kind of infrastructure question that separates forward-thinking platforms from ones that get left behind. Here's a comprehensive breakdown:

---

## 1. Publish a Machine-Readable API Spec

This is table stakes. Agents need to *discover* what your API can do.

- **OpenAPI 3.1** — Publish a `/openapi.json` or `/openapi.yaml` at a well-known URL
- Write **rich `description` fields** on every endpoint, parameter, and schema. Agents read these. Don't leave them blank.
- Use **semantic operation IDs** like `searchEventsByArtistAndDate`, not `getEvents1`

```yaml
/events/search:
  get:
    operationId: searchEventsByArtistAndDate
    description: >
      Search for upcoming ticket events by artist name, venue, city, 
      date range, or category. Returns paginated results with pricing 
      and availability metadata. Use this before calling purchaseTicket.
    parameters:
      - name: artist
        description: Full or partial artist/team name (case-insensitive)
```

---

## 2. Add an `llms.txt` File

This is the emerging standard (similar to `robots.txt`) that tells AI systems what your site does and how to interact with it.

**`https://yoursite.com/llms.txt`**
```markdown
# TicketSite API

This is a ticket marketplace for buying event tickets. 
Do not use this site to speculate or bulk-purchase.

## API
- OpenAPI spec: https://api.yoursite.com/openapi.json
- Base URL: https://api.yoursite.com/v1
- Auth: Bearer token (OAuth2)

## Key Workflows
1. Search events → GET /events/search
2. Get event details → GET /events/{id}
3. Check seat availability → GET /events/{id}/seats
4. Add to cart → POST /cart/items
5. Checkout → POST /orders
```

---

## 3. Design for Agentic Workflows, Not Just Humans

Agents chain calls. Design your API around **task completion flows**, not just CRUD.

### Hypermedia / HATEOAS
Return links to the next logical actions:

```json
{
  "event": { "id": "evt_123", "name": "Taylor Swift - Eras Tour" },
  "_links": {
    "seats": "/events/evt_123/seats",
    "add_to_cart": "/cart/items",
    "venue_map": "/venues/v_456/map"
  },
  "_actions": {
    "purchase": {
      "method": "POST",
      "href": "/orders",
      "body_schema": "/schemas/create-order"
    }
  }
}
```

### Compound / Composite Endpoints
Agents don't love chatty APIs. Add convenience endpoints:

```
GET /events/{id}/purchase-context
```
Returns event + available seats + pricing tiers + cart state in one call.

---

## 4. Build an MCP Server

This is the big one for 2025/2026. **Model Context Protocol** lets any Claude, GPT, or other agent natively call your API as a tool. Given your OpenClaw/M4 background, you already know this ecosystem well.

```typescript
// Example MCP tool definition
{
  name: "search_events",
  description: "Search for ticket events by artist, venue, city, or date. Returns events with pricing.",
  inputSchema: {
    type: "object",
    properties: {
      query: { type: "string", description: "Artist name, team, or event type" },
      city: { type: "string" },
      date_from: { type: "string", format: "date" },
      max_price: { type: "number", description: "Maximum ticket price in USD" }
    },
    required: ["query"]
  }
}
```

Host it at a `/mcp` endpoint and agents can use it *without* needing custom integration code.

---

## 5. Agent-Friendly Auth

Human OAuth flows (redirect-to-browser) break agents. Support:

| Auth Method                                          | Use Case                                                                            |
|------------------------------------------------------|-------------------------------------------------------------------------------------|
| **API Keys**                                         | Simple agent access, good for personal agents                                       |
| **OAuth2 Client Credentials**                        | Server-to-server agent flows                                                        |
| **Short-lived tokens via long-lived refresh tokens** | Persistent agents like your M4                                                      |
| **Scoped tokens**                                    | `tickets:read`, `cart:write`, `orders:create` — agents should request minimum scope |

Avoid requiring CAPTCHA or email verification mid-flow.

---

## 6. Idempotency Keys

Agents retry on failure. Without idempotency, a network timeout could result in duplicate orders.

```http
POST /orders
Idempotency-Key: agent-session-abc123-attempt-1
```

Return the same response for the same key. This is especially critical for checkout.

---

## 7. Rich, Structured Error Responses

Agents need to *decide* what to do on error. Vague errors cause hallucinations.

```json
{
  "error": {
    "code": "SEATS_UNAVAILABLE",
    "message": "The requested seats are no longer available.",
    "details": {
      "requested_seats": ["A12", "A13"],
      "event_id": "evt_123"
    },
    "suggested_actions": [
      { "action": "search_similar_seats", "href": "/events/evt_123/seats?section=A" },
      { "action": "join_waitlist", "href": "/events/evt_123/waitlist" }
    ]
  }
}
```

Use **RFC 9457 Problem Details** (`application/problem+json`) as the standard.

---

## 8. Async / Webhook Support

Some operations (seat holds, order confirmation) take time. Agents handle async better than polling.

```
POST /orders → 202 Accepted
{ "order_id": "ord_789", "status_url": "/orders/ord_789/status", "webhook_events": ["order.confirmed", "order.failed"] }
```

Also support **long-polling** as a fallback for agents that can't register webhooks.

---

## 9. Pagination That Agents Can Navigate

Use **cursor-based pagination** (not page numbers), with enough context for agents to decide whether to continue fetching:

```json
{
  "data": [...],
  "pagination": {
    "next_cursor": "eyJpZCI6MTIzfQ==",
    "has_more": true,
    "total_results": 847,
    "results_this_page": 20,
    "hint": "Consider filtering by date_range or price_max to narrow results"
  }
}
```

---

## 10. Rate Limiting with Agent Signals

Return clear rate limit headers *and* add an agent-aware tier:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 847
X-RateLimit-Reset: 1742342400
X-RateLimit-Policy: "agent-standard"
Retry-After: 30
```

Consider a separate **"agent" API tier** in your pricing/auth model — agents hit different usage patterns than humans.

---

## Priority Order

If you're building incrementally:

1. **OpenAPI spec with rich descriptions** — unlocks most agent discovery
2. **Idempotency keys** — prevents duplicate purchases (liability issue)
3. **Structured errors with suggested actions** — stops agents from hallucinating recovery paths
4. **MCP server** — puts you on the map for Claude/ChatGPT plugin ecosystems
5. **`llms.txt`** — cheap, high signal
6. **Async/webhook support** — needed for production-grade agent use cases

Given that you're already building M4 on OpenClaw with MCP integration, you're well-positioned to dog-food your own MCP server and test it directly with your personal agent setup. That's a great feedback loop.