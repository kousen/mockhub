# MockHub Demo Scenarios for Video Recordings

Scripted scenarios for repeatable demo recordings. Each scenario documents setup requirements, exact inputs, expected outputs, and recovery steps.

## Two Demo Modes

Use one of these modes deliberately:

- **Local deterministic mode**: run a fresh local dev database so seed dates are regenerated relative to now. Best when you want more predictable fixture-like behavior.
- **Live/deployed adaptive mode**: use criteria-based prompts and let the agent choose from whatever active events exist today. Best when recording against Railway or any environment with evolving data.

The scenarios below are now written to work in **live/deployed adaptive mode** by default.

**Prerequisites:**
- MockHub running locally or on Railway with `dev` profile (seed data loaded)
- Claude Desktop connected via MCP (`mcp-remote` or native OAuth2 connector)
- Admin account: `admin@mockhub.com` / `admin123`
- Demo buyer account: `buyer@mockhub.com` / `buyer123`

**Before every take:** Reset the demo user's state:
```bash
curl -X POST http://localhost:8080/api/v1/admin/demo/reset \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt>" \
  -d '{"userEmail": "buyer@mockhub.com"}'
```

Expected response:
```json
{
  "userEmail": "buyer@mockhub.com",
  "cartCleared": true,
  "cancelledOrders": [],
  "revokedMandates": []
}
```

---

## Scenario 1: Happy Path — Agentic Purchase

**Video:** Agentic Commerce (full purchase flow)
**Duration:** ~3 minutes
**Shows:** Agent-native authorization, mandate creation by the agent, search, cart, checkout, confirmation, notifications

### Setup

1. Run the demo reset for `buyer@mockhub.com`
2. Ensure no active mandates exist for the buyer
3. Open Claude Desktop connected to MockHub
4. Optional: keep the browser open and logged in as `buyer@mockhub.com` for visual confirmation at `/my/mandates` after the mandate is created

### Script

**You:** "Use MockHub to buy me a ticket to a classical concert in New York. If you need authorization first, propose sensible mandate settings and ask me."

> Agent checks for authorization / available mandates
> Agent determines none exist
> Agent proposes mandate settings:
>   - PURCHASE scope
>   - $200 per transaction
>   - $1000 total
>   - category restricted to concerts/classical
> User approves in-chat
> Agent calls `createMandate`
> Agent calls `findTickets` with criteria like `query: "classical"`, `city: "New York"`
> Expected: One or more active listings appear that satisfy the constraints, or the agent broadens the search slightly and explains the fallback

**You:** "Buy me the best orchestra or comparable seated option under $150"

> Agent calls `getBestMandate` → finds the mandate created above
> Agent narrows the current search with `section: "Orchestra"` or the closest equivalent seating filter available
> Agent calls `addToCart` with the best matching listing under budget
> Agent calls `checkout` with `paymentMethod: "pm_card_visa"`
> Agent calls `confirmOrder`
> Expected: Order confirmed, SMS + email sent to buyer

**You:** "Can you add this to my calendar?"

> Agent calls `getCalendarEntry` with the order number
> Expected: Returns .ics content

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Claude Desktop | User asks for purchase; agent says it needs authorization |
| 0:20 | Claude Desktop | Agent proposes mandate settings; user approves |
| 0:35 | Claude Desktop | Agent creates the mandate |
| 0:50 | Claude Desktop | Agent searches and finds a suitable current event |
| 1:20 | Claude Desktop | Agent adds to cart, checks out, confirms |
| 1:50 | Browser — confirmation / order history | Show confirmed order + agent attribution |
| 2:10 | Browser — `/my/mandates` | Optional: show newly created mandate and updated spending |
| 2:30 | Claude Desktop | Agent returns calendar entry |

### Recovery

If the first search is too narrow, let the agent broaden the query once while preserving the spending cap. If it picks a ticket over $200 (per-transaction limit), it will be blocked by `MandateCondition`. If something goes wrong mid-flow, run the demo reset and start over.

---

## Scenario 2: Mandate Blocks Purchase

**Video:** Agentic Commerce (authorization guardrails)
**Duration:** ~2 minutes
**Shows:** Mandate enforcement — category restriction and spending limit

### Setup

1. Use the same buyer account after Scenario 1 or create a fresh restricted mandate through the agent:
   - PURCHASE scope
   - $200 per transaction
   - category restricted to `concerts`
2. Optional: keep `/my/mandates` open in the browser for visual confirmation

### Script

**You:** "Find me tickets in a category my current mandate does not allow, like sports or theater"

> Agent calls `findTickets` with a category outside the allowed scope
> Expected: Returns listings the agent can see but should not be allowed to buy

**You:** "Buy me a ticket to that game"

> Agent calls `getBestMandate` → no mandate covers `sports` category
> Agent calls `addToCart` → `MandateCondition` returns CRITICAL: mandate does not allow category 'sports'
> Expected: Agent reports it cannot purchase sports tickets under the current mandate

**You:** "OK, find me any ticket over $200"

> Agent searches for expensive listings, or broadens to premium/floor/front-row style inventory
> Expected: Returns at least one listing above the per-transaction limit, or the agent reports none are currently available

**You:** "Buy the cheapest one"

> Agent calls `addToCart` → `MandateCondition` returns CRITICAL: transaction amount exceeds per-transaction limit of $200
> Expected: Agent reports the ticket exceeds the spending limit

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Claude Desktop | Agent searches outside the allowed category |
| 0:30 | Claude Desktop | Agent tries to buy → category blocked |
| 1:00 | Claude Desktop | Agent searches for any listing above the spend cap |
| 1:30 | Claude Desktop | Agent tries to buy → spending limit blocked |

### Recovery

No state changes to recover from — the mandate correctly blocked all purchases. Demo reset only needed if a prior take partially succeeded.

---

## Scenario 3: Mandate Lifecycle — Expiration & Revocation

**Video:** Agentic Commerce (time-bounded authorization)
**Duration:** ~2 minutes
**Shows:** Mandate expiration and human revocation

### Setup

1. Open Claude Desktop as `buyer@mockhub.com`
2. Ask the agent to create a short-lived BROWSE mandate:
   - Agent ID: `claude-desktop`
   - Scope: **BROWSE**
   - Expires: 2 minutes from now
3. Optional: keep `/my/mandates` open in the browser to show the active/expired/revoked state visually

### Script

**You:** "Search MockHub for upcoming concerts in New York"

> Agent calls `findTickets` with broad browse criteria
> Expected: Returns current active events

*(Wait for the mandate to expire — ~2 minutes. Show the clock or fast-forward in editing.)*

**You:** "Search again for upcoming concerts in New York"

> Agent calls `findTickets` → `MandateCondition` returns CRITICAL: no active mandate found
> Expected: Agent reports the mandate has expired

*(Switch to browser if you want the visual confirmation.)*

**Alternative — revocation instead of waiting:**

1. Create a new mandate through the agent with no expiration
2. Agent browses successfully
3. Switch to browser → `/my/mandates` → click **Revoke** on the active mandate
4. Switch back to Claude Desktop → agent tries to browse → blocked

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Claude Desktop | Agent creates short-lived BROWSE mandate |
| 0:20 | Claude Desktop | Agent browses — works |
| 0:40 | Browser — `/my/mandates` | Optional: show mandate in Active tab, or wait for expiry |
| 1:00 | Browser — `/my/mandates` | Optional: mandate moves to Expired tab (or click Revoke) |
| 1:20 | Claude Desktop | Agent tries again — blocked |

### Recovery

Run demo reset to clear any mandates. No orders to worry about (BROWSE-only mandate).

---

## Scenario 4: Optimistic Locking — Race Condition

**Video:** Concurrency control
**Duration:** ~2 minutes
**Shows:** Two users competing for the same ticket, `@Version`-based optimistic locking

### Setup

1. Open two browser tabs (or two browsers)
2. Tab 1: Log in as `buyer@mockhub.com` / `buyer123`
3. Tab 2: Log in as `seller@mockhub.com` / `seller123`
4. In both tabs, navigate to the same currently active event detail page with at least one available listing
5. Choose one specific listing that both users will try to buy

### Script

1. **Both tabs:** Click the same listing → "Add to Cart"
2. **Tab 1 (buyer):** Go to `/cart` → click "Checkout" → complete purchase
3. **Tab 2 (seller):** Go to `/cart` → click "Checkout"
4. **Tab 2 result:** "This ticket was just purchased by another buyer" (409 Conflict)

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Split view — both tabs | Both users viewing same event |
| 0:20 | Split view — both tabs | Both add the same listing to cart |
| 0:40 | Tab 1 (left) | Buyer checks out → success |
| 1:00 | Tab 2 (right) | Seller checks out → 409 conflict message |
| 1:20 | Tab 2 (right) | Show the user-friendly error message |

### Recovery

Run demo reset for both users:
```bash
curl -X POST .../api/v1/admin/demo/reset -d '{"userEmail": "buyer@mockhub.com"}'
curl -X POST .../api/v1/admin/demo/reset -d '{"userEmail": "seller@mockhub.com"}'
```

---

## Scenario 5: Mobile MCP — OAuth2 Discovery

**Video:** MCP on mobile
**Duration:** ~1 minute
**Shows:** OAuth 2.1 + Dynamic Client Registration enabling mobile MCP

### Prerequisites

- `mcp-oauth2` profile active on the deployed server
- Claude mobile app with MCP support
- MockHub deployed at `https://mockhub.kousenit.com`

### Script

1. Open Claude mobile app
2. Add MCP server: `https://mockhub.kousenit.com/mcp`
3. OAuth2 login flow triggers → log in as `buyer@mockhub.com`
4. Ask: "Search MockHub for concerts in New York"

> Agent calls `findTickets` with `city: "New York"`
> Expected: Returns concert listings from the phone

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Phone | Add MCP server URL |
| 0:15 | Phone | OAuth2 login page appears |
| 0:25 | Phone | Log in, authorize |
| 0:35 | Phone | Ask for concerts in New York |
| 0:50 | Phone | Results appear — MCP working from mobile |

### Recovery

No state changes — browse-only. No reset needed.

---

## Quick Reference

### Demo users

| Account | Email | Password | Role |
|---------|-------|----------|------|
| Admin | `admin@mockhub.com` | `admin123` | ROLE_ADMIN |
| Buyer | `buyer@mockhub.com` | `buyer123` | ROLE_USER |
| Seller | `seller@mockhub.com` | `seller123` | ROLE_USER |

### Demo target strategy

- Prefer **criteria-based prompts** over named-artist prompts when recording against live or long-lived environments.
- If you want deterministic named fixtures, start from a **fresh local dev database** so seeded event dates regenerate relative to the current date.
- For live recordings, let the agent select from whatever active events satisfy the budget/category/time constraints at recording time.

### Categories

| Category | Slug |
|----------|------|
| Concerts | `concerts` |
| Sports | `sports` |
| Theater | `theater` |
| Comedy | `comedy` |

### Reset command

```bash
curl -X POST http://localhost:8080/api/v1/admin/demo/reset \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt>" \
  -d '{"userEmail": "buyer@mockhub.com"}'
```
