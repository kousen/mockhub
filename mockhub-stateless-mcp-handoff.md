# MockHub: Stateless MCP Handoff

**Prepared:** August 1, 2026
**For:** Claude Code working sessions on the MockHub repository
**Status:** Planning document. Track A is authorized for implementation. Track B is explicitly blocked. Track C is design-first.

---

## Why this document exists

MockHub is a Spring Boot 4 / Spring AI 2.0 / React application (~1,000 client- and server-side tests) that serves as the demo and teaching platform for agentic commerce work. Conference-demo preparation exposed a cluster of production-boundary failures: Railway redeploys erasing in-memory MCP sessions, OAuth registration and refresh state not surviving restarts, an oversized tool surface, an in-band approval design that allowed an agent to approve its own purchase, and Spring circular dependencies visible only under the full production profile combination.

The MCP 2026-07-28 specification, published four days ago, removes protocol-level sessions. This validates MockHub's architecture (meaningful state already lives in PostgreSQL and explicit domain records) but does **not** by itself fix identity binding, credential durability, tool design, or nondeterministic agent behavior. Those problems moved up the stack, not away.

A four-hour O'Reilly Learning Platform course on agentic commerce ships in roughly one month and depends on a frozen, stable MockHub release.

**Important for any agent reading this:** the 2026-07-28 specification postdates most model training cutoffs. Do not reason about it from memory. The verified facts are in Appendix A; anything beyond that should be checked against `https://modelcontextprotocol.io/specification/2026-07-28/changelog`.

---

## Track A — Do now (no protocol dependency)

These items are correct regardless of when MockHub migrates to 2026-07-28. Several are hardening the exact failure modes that the test suite missed.

### A1. Idempotency keys on all state-changing tools

**Priority: highest.** This is the item most likely to cause a live failure and it was missed in the original analysis.

Two forces make duplicate execution more likely under the new spec. First, removing protocol sessions makes client-side retries cheaper and less visible. Second, the Multi Round-Trip Requests pattern re-issues *the same* `tools/call` after the client gathers input — duplicate delivery is now part of the normal control flow, not an error case.

Every tool that creates an order, consumes a payment credential, executes a purchase, or mutates a mandate needs an explicit idempotency key. Use the server-issued handle pattern the spec already prescribes for cross-call state: the server mints an opaque handle, the client passes it back as an ordinary tool argument.

Design-by-contract framing for the eval conditions:

- **Precondition:** a state-changing call carries an idempotency key.
- **Postcondition:** issuing the same intent with the same key N times yields exactly one order and one credential consumption.
- **Invariant:** no payment credential is consumed more than once across any retry sequence.

Audit the full tool surface for this; do not assume it is limited to checkout.

### A2. Production-profile boot smoke test in CI

The cheapest high-value test currently missing. A `@SpringBootTest` that boots the **exact** production profile combination with stubbed external credentials, asserting only that the context loads.

This catches the entire bean-cycle class of failure — the one that appeared only under the full production configuration and that ~1,000 existing tests did not see, because they run under the test profile. It should land before any expansion of the end-to-end suite.

### A3. `PurchaseProfile` as a persisted, versioned, user-visible entity

Not a transient LLM derivation. The risk in "buy tickets like last time" is not that the model misreads the instruction — it is that the model's *inference from prior orders* is unaudited.

Concrete failure: the agent reasons "you sat in section 112, so lower bowl is acceptable," then buys seats behind the stage. Fully inside the mandate, completely wrong, and invisible in the evidence record.

Requirements:

- The mandate attaches to the profile, not to the raw user utterance.
- The profile is inspectable by the customer before it is spent against.
- The profile is versioned, so evidence records reference the exact version used.
- The LLM may *propose* a profile; deterministic Java validates it against a structured schema before it becomes authoritative.

### A4. Add reversibility as an explicit mandate dimension

Current mandate reasoning is effectively one-dimensional (price tolerance). Delegated authority should scale with **reversibility**, not confidence alone.

Reordering razor blades is safe partly because evidence is strong, but mostly because a wrong order costs about twelve dollars and ships back. Tickets are non-refundable, time-boxed, and scarce. The mandate schema should carry reversibility characteristics (refundable / exchangeable / final sale, time-to-event) and the authorization decision should read them.

Model this as a matrix rather than a ladder: **authority to act** × **authority to pay**, gated on reversibility. MockHub already separates the first two; the third is missing.

### A5. Prompt-injection surface: seller-controlled listing text

MockHub already contains attacker-controllable text in listing descriptions. Add tests — and a course lab — where a listing attempts to talk the agent past its price ceiling or preferred-provider policy.

Contract to enforce: *no content originating from a listing can widen a mandate.* Mandate boundaries are evaluated in deterministic Java against structured fields only; free text from listings is never an input to an authorization decision.

This is the confused-deputy demo most agentic-commerce material skips, and MockHub is unusually well positioned to show it.

### A6. Failure-path modeling

Currently the domain stops at "order confirmed." Add the unhappy path: wrong purchase, customer dispute, refund attempt against a non-refundable listing, chargeback, and where liability sits when an agent acted inside a mandate but against intent.

This does not need to be a full implementation — it needs enough domain surface to be demonstrable and discussable.

### A7. Hostile boundary tests

The existing suite proves a great deal *inside* modeled environments. The failures clustered at boundaries it modeled thinly. Add a compact set:

1. Reconnect a real MCP client after replacing the server process.
2. Restart across persisted OAuth registration and refresh tokens.
3. Contract tests driven through a real MCP client SDK, not an in-process harness.
4. Measure wrong-tool selection rate against the current tool surface.
5. Assert an agent cannot approve, widen, or exceed its own authority.

The readiness criterion is not a larger unit-test count. It is one end-to-end path: connect a real client → authenticate → invoke tools → replace the server → reuse existing credentials → reconnect → complete a realistic purchase → verify authority boundaries held.

---

## Track B — Blocked on ecosystem support (do not implement yet)

The Java SDK is **not** in the Tier 1 group that shipped 2026-07-28 support at publication (Tier 1 is TypeScript, Python, Go, and C#). Spring AI depends on the Java SDK. Migration should wait for coordinated Java SDK + Spring AI + MCP security support.

There is no schedule pressure: publication is explicitly not a switch-off, and the new formal deprecation policy guarantees a minimum twelve-month window for anything being phased out.

**Do not:**

- Change `spring.ai.mcp.server.protocol` from `STREAMABLE`. Spring AI 2.0's older `STATELESS` transport name is **not** equivalent to 2026-07-28 compliance — it predates the spec and does not provide `server/discover`, `_meta` version negotiation, the routing headers, `resultType`, or the caching fields.
- Delete `McpSessionRecoveryFilter`. Mark it with a deprecation comment referencing the migration ticket instead. It is load-bearing until the migration actually happens.
- Rewrite protocol integration tests around stateless assumptions.

**When unblocked, the expected change set is:**

- Upgrade the coordinated dependency set; select the compliant protocol.
- Implement `server/discover` (servers MUST implement it).
- Read protocol version, client identity, and client capabilities from `_meta` on every request instead of from an `initialize` handshake.
- Emit `ttlMs` and `cacheScope` on `tools/list`, `prompts/list`, `resources/list`, `resources/read`, and `resources/templates/list` results.
- Ensure the gateway routes on `Mcp-Method` / `Mcp-Name` headers rather than session affinity.
- Delete `McpSessionRecoveryFilter`; update request logging that assumes a session identifier.
- Rewrite protocol integration tests around direct stateless calls and `server/discover`.
- Rework OAuth per Appendix A (issuer-keyed credentials, `iss` validation, `application_type`, DCR deprecation).

Tool implementations and domain services should require little change. That is the payoff of MockHub's existing design.

---

## Track C — Course-driven feature work (design first, build second)

### C1. Repeat-purchase vertical slice

Add **one narrow** slice to MockHub rather than building a second commerce backend. MockHub already has history, preferences, inventory, pricing, mandates, payment credentials, risk checks, and evidence.

Scope: *"Find tickets similar to my last order for this artist or team and buy them within a stated tolerance."*

Flow: retrieve a prior order → derive an explicit `PurchaseProfile` (A3) → combine with the new instruction → rank current inventory → execute when in bounds → request a scoped exception when not → return evidence explaining the authorization.

The LLM interprets "like last time." Deterministic Java validates a structured `RepeatPurchaseIntent` / `PurchaseProfile`. That boundary is the teaching point.

### C2. Exception approval via elicitation, not a second site visit

**This changes the demo materially.** The original analysis treated "the customer has to open MockHub and approve" as an unavoidable UX cost and answered it only at the policy layer. The 2026-07-28 spec provides a protocol mechanism.

Multi Round-Trip Requests (SEP-2322): a `tools/call` returns an `InputRequiredResult` carrying `inputRequests` (full elicitation requests) plus an opaque `requestState`. The client gathers answers and re-issues the original call with `inputResponses` and the echoed state. All state rides in the payload, so any stateless instance can resume.

Why this matters: the elicitation is fulfilled **by the client, with the human in the loop** — it is not a tool the agent can call on itself. The separation you enforced by moving approval out of the MCP tool surface is preserved by the protocol rather than by convention. The customer approves in the agent host's UI, in-conversation.

Constraints:

- Elicitation MUST NOT use form mode for sensitive credentials (passwords, API keys); URL mode is required. This bounds how a payment credential can be captured mid-flow.
- Sampling is deprecated as of 2026-07-28. Build the exception path on elicitation only.
- See A1 — MRTR re-issues the same call, so idempotency is a hard requirement here.

**MCP Apps** (SEP-1865, server-rendered HTML in a sandboxed iframe, UI actions flowing through the same audit and consent path as a direct tool call) is the complementary option: an inline mandate-boundary approval card.

Both belong in the course as first-class material, not protocol trivia. Both are Track B for implementation.

### C3. Second mock provider for the multi-provider exercise

MCP provides interoperability, not neutral marketplace arbitration. With two connected ticket services and no host policy, agent behavior is undefined — the model may prefer the better-named tool, the first-listed one, a previously successful one, query both, ask, or pick arbitrarily without disclosing alternatives.

A commercial application needs an explicit sourcing policy: search every eligible provider → normalize price, fees, seat data, refundability → deduplicate → rank against declared preferences → disclose the selection and its reason → purchase only through providers covered by the customer's authority. Affiliate relationships and preferred-provider agreements must be disclosed, or "the model chose it" conceals self-preferencing.

Express this as a postcondition rather than narrative evidence: *the selected listing is the minimum-cost listing satisfying the profile across all searched providers, or an exception record explains why not.* That makes the evidence trail verifiable.

A tiny second provider is sufficient. Do not build another ticketing platform.

### C4. Small course client

A compact Java / Spring AI client exposing prompts, records, tool choice, and validation — so students can inspect agent logic without navigating all of MockHub. This is a course artifact, not a second backend.

### C5. Course release freeze

Before course delivery: a frozen release tag, a known-good Docker image, a clean student path using mocks with no paid credentials, deterministic lab verification, post-deployment smoke tests, and demo fallbacks. Freeze the credential-free student path **first**, not last — it is the highest setup-risk item.

---

## Appendix A — Verified 2026-07-28 facts

Confirmed against the specification changelog and the release announcements, August 1, 2026.

**Breaking:**

- Protocol-level sessions and the `Mcp-Session-Id` header are removed from Streamable HTTP (SEP-2567). List endpoints no longer vary per connection. Servers needing cross-call state mint explicit, server-issued handles passed as ordinary tool arguments.
- The `initialize` / `notifications/initialized` handshake is removed (SEP-2575). Protocol version, client identity, and client capabilities travel in `_meta` on every request (`io.modelcontextprotocol/protocolVersion`, `/clientInfo`, `/clientCapabilities`). Version mismatch returns `UnsupportedProtocolVersionError`. A new `server/discover` RPC, which servers MUST implement, advertises supported versions, capabilities, and identity.
- Blocking `tasks/result` is removed; Tasks moves to an official extension with polling via `tasks/get` (SEP-2663).

**New requirements:**

- `Mcp-Method` and `Mcp-Name` headers are required on Streamable HTTP POSTs (SEP-2243), so infrastructure can route and rate-limit without inspecting bodies. Custom headers from tool parameters via `x-mcp-header`.
- `CacheableResult` requires `ttlMs` (freshness hint, milliseconds) and `cacheScope` (`"public"` or `"private"`) on list and read results (SEP-2549).
- Every result carries a required `resultType`: `"complete"` or `"input_required"` (SEP-2322). Results from earlier-protocol servers are treated as complete.

**Authorization hardening:**

- Authorization servers SHOULD include `iss` in authorization responses; clients MUST validate a present `iss` against the recorded issuer before redeeming the code (SEP-2468).
- Clients MUST specify an appropriate `application_type` at registration, to avoid OIDC redirect URI conflicts (SEP-837).
- Client credentials are bound to the issuing authorization server. Clients MUST key persisted credentials by issuer and re-register when the AS changes (SEP-2352). **This is the same problem as MockHub's redeploy-durability issue, seen from the other end.**
- OAuth 2.0 Dynamic Client Registration is formally **deprecated** in favour of Client ID Metadata Documents. It still works for backwards compatibility but is slated for removal.

**Deprecated (not removed; minimum twelve-month window):**

- Roots, Sampling, and Logging (SEP-2577).
- The older HTTP+SSE transport, reclassified as Deprecated (SEP-2596).

**Ecosystem status:**

- Tier 1 SDKs speaking 2026-07-28 at publication: TypeScript, Python, Go, C#. **Java is not among them.**
- Formal feature lifecycle policy (SEP-2596): Active / Deprecated / Removed, twelve-month minimum deprecation window, ninety-day floor even for expedited security removals.

---

## Appendix B — Open questions

Not yet decided; do not assume answers.

1. Exact `RepeatPurchaseIntent` / `PurchaseProfile` field set.
2. How seat similarity normalizes across events and venues.
3. Which price tolerances are inferred versus explicitly stated by the customer.
4. Whether the agent may place temporary holds before requesting an exception.
5. Smallest useful second-provider simulation.
6. Which MCP/OAuth steps students execute versus observe.
7. Final module and exercise timing for the four-hour slot.

---

## Appendix C — Reference framing

Amazon's Auto Buy constraints read as a mandate specification written in customer-service prose: Prime members only, Fulfilled-by-Amazon items only, one active request per item, one unit per item, no promotional discounts or coupons, up to 200 active requests, cancellable any time before the order is placed.

Note also that Amazon's *scheduled recurring purchases* add items to the cart for customer review rather than shipping automatically — unlike Subscribe and Save. The company with maximum incentive to remove friction chose full autonomy only for the narrow single-SKU, price-triggered case. That is evidence for tightly constrained mandates, not against human checkpoints.

MockHub's harder problem: it cannot repeat a SKU. It must interpret "like last time" across unique, non-fungible, non-refundable inventory.
