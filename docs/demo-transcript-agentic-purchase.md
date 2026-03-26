# Agentic Purchase Demo — Claude Desktop + MockHub MCP

**Date:** 2026-03-26
**Client:** Claude Desktop (Opus 4.6) via `mcp-remote` bridge
**Server:** MockHub MCP (Streamable HTTP, Railway)

## Context

This transcript shows the complete agentic purchase flow: from natural language query through ticket search, seat selection, mandate authorization, cart management, checkout, and cleanup. Some steps experienced significant latency — performance profiling is needed to identify bottlenecks in the MCP transport and query layers.

---

## Transcript

**User:** Use mockhub to find classical concerts in New York in the next month

**Claude:** *(Used FindTickets — no results for "classical" category slug, pivoted to text search)*

MockHub has one classical concert in New York within the next month:

**Yo-Yo Ma — Bach Cello Suites**
- Date: April 23, 2026 at 9:00 PM (doors open at 8:00 PM)
- Venue: Radio City Music Hall, New York, NY
- Description: The world's most renowned cellist performs Bach's beloved Cello Suites
- Tickets: $127.50 – $375.00 (base price $150), with 6,013 tickets still available
- Tags: Classical, Indoor

---

**User:** What tickets are available for that concert?

**Claude:** *(Used GetEventListings — 508 active listings)*

Here's the full breakdown — 508 active listings across four sections:

- **Orchestra** — 227 tickets, $120.23 – $179.22, Rows A through T
- **First Mezzanine** — 104 tickets, $121.28 – $179.60, Rows A through L
- **Second Mezzanine** — 97 tickets, $120.02 – $179.50, Rows A through J
- **Third Mezzanine** — 80 tickets, $120.98 – $179.88, Rows A through H

All tickets are standard type, and the pricing is surprisingly flat across sections — everything falls in the $120–$180 range regardless of where you sit. That's a pretty narrow band, which makes Orchestra the obvious value play since you're getting the best seats for essentially the same price.

---

**User:** Find me a pair of adjacent seats in the Orchestra section

**Claude:** *(Analyzed listing data client-side — no dedicated tool for adjacent pair search)*

Found 79 adjacent pairs in Orchestra. Standout options:

**Best overall value:**
- Row D, Seats 5–6 — $121.07 + $127.86 = $248.93 total (cheapest pair)
- Row C, Seats 28–29 — $120.46 + $129.79 = $250.25 total
- Row H, Seats 17–18 — $127.70 + $124.10 = $251.80 total

**Best front-row options:**
- Row A, Seats 27–28 — $125.90 + $128.91 = $254.81 total (front row!)
- Row A, Seats 7–8 — $146.38 + $143.80 = $290.18 total

---

**User:** Let's buy those seats you suggested in Row D

**Claude:** To add those Row D Seats 5–6 to your cart, I'll need your mockhub email address and a mandate (authorization for me to act on your behalf).

**User:** My mockhub email address is ken.kousen@kousenit.com. I don't know anything about mandate sets

**Claude:** A mandate is MockHub's way of authorizing me to take actions on your behalf. I'd scope it narrowly:
- Scope: PURCHASE
- Allowed event: Just this Yo-Yo Ma concert
- Max spend: ~$250
- Expiration: 1 hour from now

**User:** Yes

**Claude:** *(Created mandate → Found listing IDs → Added both seats to cart)*

Both seats are in your cart:

- Orchestra Row D, Seat 5 — $102.91
- Orchestra Row D, Seat 6 — $108.68
- Subtotal: $211.59

*(Computed prices came in lower than listed prices due to dynamic pricing engine)*

**User:** *(Proceeded to checkout)*

**Claude:** *(Checkout → Confirm → Revoke mandate)*

**Order #MH-20260326-0002 — CONFIRMED**
- Event: Yo-Yo Ma — Bach Cello Suites
- Date: April 23, 2026 at 9:00 PM
- Venue: Radio City Music Hall, New York
- Seats: Orchestra Row D, Seats 5 & 6
- Total paid: $232.75

Mandate revoked for safety.

---

## MCP Tools Used (in order)

1. `findTickets` — initial search (no results for "classical" slug)
2. `searchEvents` — fallback text search
3. `getEventDetail` — full event details
4. `getEventListings` — all 508 listings for the event
5. *(client-side analysis)* — adjacent pair finding from listing data
6. `createMandate` — scoped authorization (PURCHASE, one event, $250 cap, 1 hour)
7. `findTickets` — locate specific listing IDs for Row D seats
8. `addToCart` — Seat 5 (×1)
9. `addToCart` — Seat 6 (×1)
10. `checkout` — create order
11. `confirmOrder` — finalize purchase
12. `revokeMandate` — cleanup authorization

## Observations

- **Agent resilience:** Claude recovered from the "classical" category slug miss by switching to text search
- **Client-side reasoning:** Adjacent seat analysis was done by Claude from raw listing data — no server-side tool needed
- **Mandate scoping:** Claude narrowly scoped the mandate without being told to (one event, $250, 1 hour)
- **Dynamic pricing:** Computed prices ($102.91, $108.68) were lower than listed prices ($121.07, $127.86) due to the pricing engine
- **Performance:** Several steps had significant latency — profiling needed to identify bottlenecks in MCP transport vs. query execution

## Known Issues During Demo

- `findTickets` initially failed because tool descriptions listed wrong category slugs (`'rock'` instead of `'concerts'`) — fixed in commit 8782bcc
- `mcp-remote` process persisted across Claude Desktop toggle, requiring full app restart to clear stale session
- Session recovery filter (HTTP 404 on stale sessions) deployed mid-session
