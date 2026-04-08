# MockHub Demo Scenarios for Video Recordings

Scripted scenarios for repeatable demo recordings. Each scenario documents setup requirements, exact inputs, expected outputs, and recovery steps.

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
**Shows:** Mandate creation, agent search, cart, checkout, confirmation, notifications

### Setup

1. Log in as `buyer@mockhub.com` in the browser
2. Navigate to `/my/mandates` and click **New Mandate**
3. Fill in the form:
   - Agent ID: `claude-desktop`
   - Scope: **Purchase**
   - Per-Transaction Limit: `200`
   - Total Budget: `1000`
   - Allowed Categories: `concerts`
   - Expires: *(set to 1 hour from now)*
4. Click **Create Mandate** — note the mandate appears in the Active tab
5. Switch to Claude Desktop

### Script

**You:** "Use MockHub to find classical concerts in New York"

> Agent calls `findTickets` with `query: "classical"`, `city: "New York"`
> Expected: Yo-Yo Ma — Bach Cello Suites shows up (~$120–$180 range)

**You:** "Buy me a ticket to the Yo-Yo Ma concert, something in the Orchestra section under $150"

> Agent calls `getBestMandate` → finds the mandate created above
> Agent calls `findTickets` with `query: "Yo-Yo Ma"`, `section: "Orchestra"`, `maxPrice: 150`
> Agent calls `addToCart` with the cheapest listing
> Agent calls `checkout` with `paymentMethod: "pm_card_visa"`
> Agent calls `confirmOrder`
> Expected: Order confirmed, SMS + email sent to buyer

**You:** "Can you add this to my calendar?"

> Agent calls `getCalendarEntry` with the order number
> Expected: Returns .ics content

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Browser — `/my/mandates` | Human creates the mandate (the "permission slip") |
| 0:30 | Claude Desktop | Agent searches, finds Yo-Yo Ma |
| 1:00 | Claude Desktop | Agent adds to cart, checks out |
| 1:30 | Browser — `/orders` | Show the confirmed order appeared |
| 2:00 | Claude Desktop | Agent returns calendar entry |
| 2:30 | Browser — `/my/mandates` | Show spending updated on the mandate |

### Recovery

If the agent picks a ticket over $200 (per-transaction limit), it will be blocked by `MandateCondition`. Ask for a cheaper ticket. If something goes wrong mid-flow, run the demo reset and start over.

---

## Scenario 2: Mandate Blocks Purchase

**Video:** Agentic Commerce (authorization guardrails)
**Duration:** ~2 minutes
**Shows:** Mandate enforcement — category restriction and spending limit

### Setup

1. Same mandate as Scenario 1: `claude-desktop`, PURCHASE scope, $200/transaction, `concerts` category only
2. Switch to Claude Desktop

### Script

**You:** "Find me tickets to the next NBA game"

> Agent calls `findTickets` with `category: "sports"`
> Expected: Returns sports events (NBA games)

**You:** "Buy me a ticket to that game"

> Agent calls `getBestMandate` → no mandate covers `sports` category
> Agent calls `addToCart` → `MandateCondition` returns CRITICAL: mandate does not allow category 'sports'
> Expected: Agent reports it cannot purchase sports tickets under the current mandate

**You:** "OK, find me front-row tickets to Taylor Swift"

> Agent calls `findTickets` with `query: "Taylor Swift"`, `section: "Floor"`
> Expected: Listings at ~$400+ (Taylor Swift base price is $450)

**You:** "Buy the cheapest one"

> Agent calls `addToCart` → `MandateCondition` returns CRITICAL: transaction amount exceeds per-transaction limit of $200
> Expected: Agent reports the ticket exceeds the spending limit

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Claude Desktop | Agent searches sports → finds NBA games |
| 0:30 | Claude Desktop | Agent tries to buy → category blocked |
| 1:00 | Claude Desktop | Agent searches Taylor Swift → finds expensive tickets |
| 1:30 | Claude Desktop | Agent tries to buy → spending limit blocked |

### Recovery

No state changes to recover from — the mandate correctly blocked all purchases. Demo reset only needed if a prior take partially succeeded.

---

## Scenario 3: Mandate Lifecycle — Expiration & Revocation

**Video:** Agentic Commerce (time-bounded authorization)
**Duration:** ~2 minutes
**Shows:** Mandate expiration and human revocation

### Setup

1. Log in as `buyer@mockhub.com`
2. Create a mandate at `/my/mandates`:
   - Agent ID: `claude-desktop`
   - Scope: **Browse**
   - Expires: *(set to 2 minutes from now)*
3. Switch to Claude Desktop

### Script

**You:** "Search MockHub for jazz concerts"

> Agent calls `findTickets` with `query: "jazz"`
> Expected: Returns jazz events (Wynton Marsalis, Kamasi Washington, etc.)

*(Wait for the mandate to expire — ~2 minutes. Show the clock or fast-forward in editing.)*

**You:** "Search again for jazz concerts"

> Agent calls `findTickets` → `MandateCondition` returns CRITICAL: no active mandate found
> Expected: Agent reports the mandate has expired

*(Switch to browser)*

**Alternative — revocation instead of waiting:**

1. Create a new mandate with no expiration
2. Agent browses successfully
3. Switch to browser → `/my/mandates` → click **Revoke** on the active mandate
4. Switch back to Claude Desktop → agent tries to browse → blocked

### What to show on screen

| Time | Screen | What's happening |
|------|--------|-----------------|
| 0:00 | Browser — `/my/mandates` | Create short-lived mandate |
| 0:20 | Claude Desktop | Agent browses — works |
| 0:40 | Browser — `/my/mandates` | Show mandate in Active tab, or wait for expiry |
| 1:00 | Browser — `/my/mandates` | Mandate moves to Expired tab (or click Revoke) |
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
4. Both navigate to the same event detail page (e.g., `/events/yo-yo-ma-bach-cello-suites-1`)

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

### Key events for demos

| Event | Slug | Category | Base Price |
|-------|------|----------|-----------|
| Yo-Yo Ma — Bach Cello Suites | `yo-yo-ma-bach-cello-suites-*` | concerts | $150 |
| Wynton Marsalis Quintet | `wynton-marsalis-quintet-*` | concerts | $65 |
| Taylor Swift — Eras Tour | `taylor-swift-eras-tour-*` | concerts | $450 |

*Note: Slug suffixes are numeric based on seed order. Use `findTickets` to discover exact slugs.*

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
