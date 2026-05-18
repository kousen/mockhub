# Agentic Commerce Strategy for MockHub and the August Training Course

**Date:** May 13, 2026
**Status:** Decisions captured; implementation underway
**Suggested home in repo:** `docs/strategy/2026-05-13-agentic-commerce.md`

## 1. Context

On May 12–13, 2026, Nate B. Jones published a video and accompanying Substack post on the agentic commerce protocol landscape. The piece argues that the old online purchase worked because every party agreed on the *shape of the evidence*: a human was present, a page was shown, a credential was used, a final action was taken. Agentic commerce shatters that consensus before it shatters the rails. Software that buys on behalf of a person or business unbundles the checkout click into a set of distinct commercial responsibilities — identity, authorization, payment credential, settlement, governance, liability — and a different protocol camp is staking out each one.

The Substack adds a material fact the video buries: OpenAI and Stripe shipped Instant Checkout inside ChatGPT on September 29, 2025, and OpenAI scaled it back about five months later. Etsy reported low volume; Walmart's head of AI told Wired that conversion inside the chatbot ran roughly three times lower than the same items requiring a click-out. The infrastructure problems — real-time inventory sync, sales tax, fraud, multi-item carts — turned out harder than the demo suggested. OpenAI's revised position: ChatGPT does discovery, merchants own checkout in their own surfaces.

But the protocol itself survived the product retreat. ACP is on its fifth spec revision, with PayPal joining as a payment provider in October 2025 and Salesforce in shortly after; Shopify supports both ACP and UCP. The agent-to-merchant data-passing layer is the part that endures across product failures.

This document captures the strategy decisions that follow from that landscape, the specific issues filed against the MockHub repo as a result, and how those issues sequence into the August training course.

## 2. The Protocol Landscape

Six layers, with current camps at each:

1. **Agent surface and discovery** — Where the buyer expresses intent. OpenAI's ACP focuses on the agent-to-merchant checkout flow.
2. **Merchant control** — Shopify and Google's UCP, broader in scope, tries to keep discovery, ranking, bundling, loyalty, returns, and the post-purchase relationship on the merchant side rather than absorbed by the assistant.
3. **Delegated authorization** — Google's AP2 introduces the *mandate*: a structured record of what the user authorized the agent to do (scope, constraints, proof of approval). Stripe shipped a consumer-facing instance of the same idea in April 2026 with the Link wallet for agents.
4. **Network trust and tokenized credentials** — Visa, Mastercard, and PayPal pursuing agent registration, payment tokens, and dispute infrastructure from their existing positions.
5. **Machine-to-machine rails** — Coinbase's x402 (HTTP 402 activated as a payment-required status), Stripe's Machine Payments Protocol, and Circle Arc for stablecoin settlement. Targets software-pays-software flows, not retail.
6. **Enterprise runtime and governance** — AWS Bedrock AgentCore Payments (announced May 7, 2026), built with Coinbase and Stripe. AWS owns the agent runtime, not a single protocol — the runtime sees task, tools, policy, budget, and history, which is leverage no payment provider has.

The unifying question across all six is *where does responsibility live?* When the agent finds the product, who owns the recommendation? When it requests permission, who records what the user approved? When it pays, who owns the credential and the risk? When the buyer wants a refund, who handles the return?

These don't have one answer. They divide across layers, which is exactly why protocols multiply.

## 3. Where MockHub Stands

As of May 18, 2026, MockHub already implements substantial parts of the agentic-commerce stack:

- **MCP server** with 33 tools covering events, cart, orders, pricing, mandates, approvals, payment credentials, and risk summaries
- **ACP checkout endpoints** at `/acp/v1/checkout/**`
- **Agent mandates** with scope and spending limits
- **Scoped payment credentials** with mock-backed instruments, agent/user/amount/currency constraints, one-time consumption, and ACP/MCP checkout validation
- **Agent risk signals** for rapid cart holds, mandate mismatches, failed checkouts, high-spend attempts, and payment-credential failures
- **Buyer preference memory** for explicit ticket-shopping preferences used by MCP discovery/comparison and AI recommendations
- **OAuth 2.1 / MCP security** via `spring-ai-community/mcp-security`
- **Agent discovery** through `llms.txt`
- **Evaluation conditions** as Design-by-Contract sanity checks for AI agents

What MockHub does *not* yet implement:

- Stripe-backed wallet or Link-style credentials
- A unified evidence trail across mandate, credential, checkout, and fulfillment
- UCP capability declarations
- An agent-side client that exercises the full stack

## 4. What We Decided To Build

Eight issues now tracked in the backlog or decision log, grouped by what they accomplish.

### Building the parts

- **#217** — Agent risk and abuse signals. Tracks repeated mandate mismatches, failed checkouts, rapid cart holds, high-spend attempts, and payment-credential failures. Integrates with eval conditions as WARNING or CRITICAL and exposes `getAgentRiskSummary`.
- **#218** — Scoped payment credential abstraction *distinct from* mandates. Mandates answer "is this agent allowed to act?"; credentials answer "is this agent allowed to pay with this instrument, under these constraints?" Closer to the Stripe Link wallet pattern.
- **#219** — Buyer preference memory. Lets agents translate fuzzy intent into a structured purchasing brief. Adds explicit preference storage, authenticated update/read APIs, preference-aware MCP search/comparison, and recommendation metadata.

### Building the evidence layer

- **#238** — Agent purchase evidence trail view. A single read-only artifact that assembles mandate, approval record, scoped credential, checkout, risk signals, fulfillment, and eval condition outcomes for a given order. This is the keystone: it is the *new shape of the evidence* that Nate identifies as the missing piece, made concrete in code.

### Making it visible to students

- **#239** — Agent client demo module. A small Spring AI client that exercises the full stack as an external agent would — mandate → credential → ACP checkout → evidence trail read. Optional parallel Python client for the August Python-curious students. This is what the course actually shows running.
- **#236** — UCP readiness documentation. Maps MockHub surfaces to UCP concepts without claiming UCP compliance. Explicit table of what's implemented, partial, planned, and intentionally out of scope.

### Keeping it honest

- **#237** — UCP discovery profile spike (decision captured: not yet). Investigated whether a `/.well-known/ucp` manifest is worth publishing and concluded that a profile should wait until MockHub has UCP-shaped service bindings, buyer-context decisions, and profile-content tests. Scoped payment credentials (#218), risk signals (#217), and pinned protocol revisions (#240) now provide key prerequisites; a misleading manifest is still worse than none, especially for a teaching repo.
- **#240** — Spec version pins (captured in `docs/agentic-commerce.md`). Records which ACP / UCP / AP2 / x402 revisions MockHub targets, with last-verified dates. Protocols are churning weekly; without this pin, course materials silently age out of date.

## 5. The August Course Arc

Nate offers a useful concept the video buries in the closing: *graduated autonomy*. Agents act freely on low-risk transactions, request approval on moderate-risk, and prepare evidence for human review on high-risk. This maps cleanly to a three-tier course arc.

**Module 1 — The parts of an agentic purchase.** Walk through the six-layer protocol landscape from this document. Read the UCP doc (#236) and the spec pins (#240) together. Run the existing MCP server and call its tools by hand. No new code; this is orientation.

**Module 2 — Low-risk tier: the agent acts freely.** Students extend the agent client demo (#239) to make a small, fully-mandated purchase under a bounded budget. The mandate, credential, and checkout flow run with no human in the loop. Students inspect the evidence trail (#238) and see what evidence the system produced.

**Module 3 — Moderate-risk tier: the agent asks for approval.** Mandates are scoped tightly enough that the agent has to request a new mandate or wait for an approval record when crossing a budget or merchant boundary. Risk signals (#217) start appearing; students see what triggers a WARNING and what blocks an action.

**Module 4 — High-risk tier: the agent prepares evidence for human review.** Students introduce a deliberately ambiguous purchase intent. The agent assembles a complete evidence record using #238 and surfaces it for review. Discussion: what would a dispute look like? What does the audit trail need to contain? This is where the Design-by-Contract framing pays off — preconditions, postconditions, and audit are the same idea the students recognize from Eiffel.

**Module 5 — What we deliberately didn't build.** Read the out-of-scope section of #236 and the Module 4 framing. Why isn't MockHub trying to be UCP-compliant? Why no x402? Why no Bedrock AgentCore wrapper? The teaching point: knowing which layer you are *not* playing in is as strategic as knowing which one you are.

This arc is sequential but each module can stand alone for a corporate one-day variant.

## 6. Out of Scope

Decisions made explicitly *not* to chase in this cycle:

- **Full UCP compliance.** The spec is broad and churning; honest partial implementation is the only safe move (#236 and #237).
- **x402 and stablecoin rails.** Doesn't fit the ticket-marketplace domain. Could be a separate demo repo later if the Python-curious students want a machine-pays-machine sample.
- **AWS Bedrock AgentCore integration.** Cloud-platform-specific; not portable to the multi-cloud audience for the August course.
- **A production-grade agent client.** The demo in #239 is a teaching artifact, not infrastructure.
- **Visa / Mastercard / PayPal agent registration.** Network-trust layer; out of scope for a Spring/Java demo of the protocol concepts.

## 7. Open Questions

Things worth revisiting before or during course prep:

- **AP2 mandate shape.** MockHub's mandate predates the public AP2 spec. After #218 lands, worth a comparison pass: do field names align? Does the signature handling match? If close, document the mapping in #236. If diverged, file a follow-up issue.
- **Dispute and refund flow for agent-initiated orders.** Not in the backlog yet. Nate emphasizes this throughout; MockHub has order management but no agent-specific dispute path. Probably a future issue once #238 is in.
- **Claude Desktop bearer-token gap.** The recurring authentication issue from earlier work. The mandate-plus-credential split in #218 may incidentally help, since the mandate can carry its own authorization context independent of the transport. Worth testing once #218 is in.
- **Whether to publish a sibling Python repo.** Mentioned as optional in #239. Decision can wait until the Java client is working end-to-end.

## 8. References

- Nate B. Jones, "Six layers your agent has to handle. Most products have only thought about two." Substack post, May 12, 2026.
- Nate B. Jones, video on agentic commerce, YouTube, May 12, 2026. (`https://youtu.be/j5_wcDifNko`)
- Google Developers Blog, "Under the Hood: Universal Commerce Protocol (UCP)," January 11, 2026.
- Shopify Engineering, "Building the Universal Commerce Protocol (2026)."
- Google for Developers, Universal Commerce Protocol Guide (`developers.google.com/merchant/ucp`).
- MockHub backlog: issues #217, #218, #219, #236, #237, #238, #239, #240.

---

*Maintainer note: this document is a snapshot of strategic intent on May 13, 2026. Specifics (issue numbers, spec revisions, dated facts) will age. The framing — six layers, evidence-as-shape, graduated autonomy as course arc — is intended to outlast the specifics.*
