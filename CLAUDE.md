# MockHub — Project Instructions for Claude Code

## Project Overview

MockHub is a secondary concert ticket marketplace (like StubHub) built as a teaching platform for undergraduate AI students. The codebase must be clean, well-organized, and approachable for students reading it for the first time.

## Tech Stack

- **Backend:** Spring Boot 4, Java 25, Gradle 9.4.0 (Kotlin DSL)
- **Database:** PostgreSQL 17 (standard — pgvector removed, all search uses tsvector full-text)
- **AI:** Spring AI 2.0.0-M3 (milestone — requires Spring Milestones repo in Gradle)
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack React Query, Zustand
- **Testing:** JUnit 5 + Mockito + Testcontainers (backend), Vitest + React Testing Library + MSW (frontend), Playwright (E2E, 3 browsers, sharded CI)
- **Payments:** Stripe test mode + mock fallback via Spring profiles

## Architecture Rules

### Backend

- **Feature-based packages**, not layer-based. Each domain concept (`event`, `ticket`, `cart`, `pricing`, etc.) is a self-contained package with controller/service/repository/entity/dto sub-packages.
- **Controller → Service → Repository** layer discipline:
  - Controllers never access repositories directly
  - Services call other services, never controllers
  - Entities never leak outside the service layer — use DTOs for all external communication
- **DTOs are Java records.** Use `*Request` records for incoming data (with Jakarta validation), `*Dto` for responses, `*SummaryDto` for list views.
- **Mapping is explicit** in service methods. No MapStruct, no Lombok. Keep it transparent for students.
- **No Lombok.** Java records for DTOs, explicit getters/setters for JPA entities. Students should see what the code does.
- **All write operations** use `@Transactional`. Read-only operations use `@Transactional(readOnly = true)`.
- **Database migrations** use Flyway (never JPA auto-DDL in any profile except `validate`).
- **Error handling** uses RFC 9457 Problem Details via `GlobalExceptionHandler` (`@RestControllerAdvice`). Returns `ProblemDetail` objects with `type`, `title`, `status`, `detail` fields. Domain exceptions are modeled as a sealed hierarchy (see DOP section).
- **Caching:** Use Spring `@Cacheable` annotations. Never cache carts, orders, notifications, or pricing data.
- **Spring profiles** control implementation variants:
  - `dev` — PostgreSQL, mock payment, mock SMS/email, debug logging, AI disabled (default). Profile group: `dev,mock-payment,mock-sms,mock-email`
  - `test` — Testcontainers PostgreSQL, mock payment
  - `mock-payment` / `stripe` — payment implementation (`@Primary` on Stripe resolves conflicts when both active)
  - `ai-anthropic` / `ai-openai` / `ai-ollama` — AI provider (each overrides the base AI exclusion list to enable only its own provider)
  - `mcp-oauth2` — OAuth 2.1 with DCR on MCP endpoints (replaces API key auth). Without it, falls back to `X-API-Key` header.
  - Base `application.yml` excludes all AI auto-configs. Each `ai-*` profile overrides with a narrower list.
  - To enable AI: `SPRING_PROFILES_ACTIVE=dev,ai-anthropic` (just add the provider to dev)

### Data Oriented Programming

The codebase uses Java DOP patterns where they add value:

- **Sealed exception hierarchy:** `DomainException` is `abstract sealed`, permitting `ResourceNotFoundException`, `ConflictException`, `PaymentException`, `UnauthorizedException` (all `final`). The `GlobalExceptionHandler` uses an exhaustive pattern-matching switch with no default case.
- **Records for all DTOs:** 43 out of 43 DTOs are records. `EventSearchRequest` uses a compact constructor to apply defaults and clamp bounds.
- **`PaymentService` is intentionally NOT sealed.** Mockito cannot mock sealed interfaces, which breaks `@WebMvcTest` controller tests. Testability wins over closed polymorphism here.
- **JPA entities cannot be records** (require mutable state, no-arg constructors). They use explicit getters/setters with `BaseEntity` superclass.

### AI Features

- **Conditional activation:** AI services use `@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")` (NOT `@ConditionalOnBean` — that evaluates before auto-config creates the beans). The `AiController` injects `Optional<ChatService>` etc. and returns 503 when no AI provider is active.
- **Profile activation:** `SPRING_PROFILES_ACTIVE=dev,ai-anthropic` — just add the AI provider to dev.
- **Services:** `ChatService` (chat assistant with 10-message `MessageWindowChatMemory`), `PricePredictionService` (price trend analysis), `RecommendationService` (AI-ranked event recommendations).
- **ChatClient has function-calling tools.** `EventTools` and `PricingTools` are wired into the ChatClient via `.defaultToolCallbacks(ToolCallbacks.from(...))`. The same `@Tool`-annotated classes serve both the MCP server (external agents) and the chat endpoint (users on the website).
- **AI responses are parsed from JSON.** Services prompt the LLM for JSON output and parse with Jackson. Fallback logic returns safe defaults if parsing fails.
- **Circular dependency:** MCP tool registration → PricingTools → PricePredictionService → ChatClient creates a cycle. Broken with `@Lazy` on the `ChatClient` parameter in `PricePredictionService`.
- **ChatContext email enforcement:** Website chat tool calls use `ChatContext` (ThreadLocal) to override the LLM-provided `userEmail` with the authenticated user's email. Prevents prompt injection from operating as a different user. External MCP calls don't set the context, so `userEmail` is honored as-is. All tool classes use `ChatContext.resolveEmail(paramEmail)` for consistent resolution.
- **Circular dependency (eval variant):** When a bean that depends on `AnthropicChatModel` (like `evalJudgeChatClient`) is injected into a `@Component` collected via `List<EvalCondition>`, Spring eagerly resolves the entire list during context startup — `@Lazy` on the component class or the list parameter does NOT prevent this. Fix: remove `@Component` from the condition class and define it as a `@Bean` in a `@Configuration` class with `@Lazy` on the `ChatClient` parameter. This ensures the `ChatClient` proxy is only resolved at first method call, after the context is fully initialized. Unit tests won't catch this because they mock dependencies; only a full `@SpringBootTest` or production deploy with all profiles active will trigger the cycle.

### Evaluation Conditions

- **Design by Contract for AI agents.** Evaluation conditions are formalized sanity checks (preconditions, postconditions, invariants) that encode domain judgment. They prevent AI agents from taking technically correct but contextually inappropriate actions.
- **Package: `com.mockhub.eval`** — top-level package (cross-cutting, used by both AI services and MCP tools). Contains `EvalCondition` interface, DTOs (`EvalResult`, `EvalContext`, `EvalSeverity`, `EvalSummary`), concrete conditions in `condition/`, `EvalRunner` service in `service/`, config in `config/`.
- **Two types of conditions:** Deterministic (rule-based, always run, zero cost) and AI-as-judge (second LLM call, opt-in via `mockhub.eval.ai-judge.enabled`).
- **`EvalCondition` is NOT sealed** — must be mockable with Mockito, and extensible for students adding their own conditions.
- **Explicit calls, not AOP.** Services call `evalRunner.evaluate(context)` visibly. Students read the code and see where judgment happens.
- **Eval results are records, not exceptions.** Callers decide what to do — block (CartTools), fallback (PricePredictionService), or log-only (ChatService).
- **Separate `evalJudgeChatClient` bean** in `EvalConfig` — no tools, no memory, avoids circular dependency with main ChatClient.
- **Documentation:** `docs/evaluation-conditions.md` covers the concept, Design by Contract lineage, Nate Jones's contextual stewardship framework, and how to add new conditions.

### SMS Delivery

- **Profile-based SMS:** `SmsDeliveryService` interface with `MockSmsDeliveryService` (console logging, `mock-sms` profile) and `TwilioSmsDeliveryService` (real SMS, `sms-twilio` profile, `@Primary`).
- **`sendSms()` returns the provider message SID** (or null on failure) for delivery tracking.
- **Triggered on order confirmation:** `OrderService.confirmOrder()` sends SMS if user has a phone number. Message includes event name and link to public ticket view page (token-based, no auth required).
- **SMS failures are caught and logged** — never break the checkout flow.
- **Twilio SDK:** `com.twilio.sdk:twilio:10.9.2`. Auth via `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN` env vars.
- **Phone number:** Optional field on `RegisterRequest`. Already on `User` entity. Frontend registration form includes it.
- **Integration test:** `TwilioSmsDeliveryServiceIntegrationTest` tagged `@Tag("twilio")`, excluded from normal test runs. Run with `./gradlew test -PincludeTags=twilio` (requires `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER` env vars).
- **Dev profile group:** `dev: dev,mock-payment,mock-sms,mock-email` — mock SMS and email log to console by default.

### Email Delivery

- **Profile-based email:** `EmailDeliveryService` interface with three implementations:
  - `MockEmailDeliveryService` (console logging, `mock-email` profile)
  - `SmtpEmailDeliveryService` (Spring `JavaMailSender`, `email-smtp` profile) — works with any SMTP provider
  - `ResendEmailDeliveryService` (Resend REST API, `email-resend` profile, `@Primary`) — uses Spring `RestClient` to POST to `https://api.resend.com/emails`. Preferred in production because Railway blocks SMTP port 465.
- **Triggered on order confirmation:** `OrderService.confirmOrder()` sends HTML email with order summary and "View Your Tickets" button linking to the public ticket view page.
- **Email failures are caught and logged** — never break the checkout flow.
- **From address:** Configurable via `mockhub.email.from-address` (env: `EMAIL_FROM_ADDRESS`). Production uses `tickets@updates.kousenit.com` (verified domain in Resend).
- **Multiple constructors:** `ResendEmailDeliveryService` has a package-private constructor for testing. The public constructor uses `@Autowired` to resolve ambiguity (Spring requires this when multiple constructors exist).

### Public Ticket View

- **Token-authenticated public page** at `/tickets/view?token={orderViewToken}` — no login required. Shows scannable QR codes for all tickets in an order.
- **Order-view JWT tokens:** Signed with the same HMAC-SHA256 key as ticket verification tokens, but distinguished by a `typ: "order-view"` claim. Generated by `TicketSigningService.generateOrderViewToken()`. Cannot be used to scan/verify tickets (no `tic` claim).
- **Two public endpoints:** `GET /api/v1/tickets/view?token=...` returns order + ticket details as `PublicOrderViewDto`. `GET /api/v1/tickets/{orderNumber}/{ticketId}/qr?token=...` returns QR code PNG image. Both validate the order-view token.
- **QR codes are the same as PDF tickets** — scanning at a venue triggers the existing `/api/v1/tickets/verify` flow with scan tracking.
- **SMS and email both link here** — replaces the old authenticated `/orders/{orderNumber}/confirmation` link.
- **Mobile-optimized frontend:** `PublicTicketViewPage` shows event name, date, venue, and large QR code cards. No PII exposed (no buyer name/email/phone).

### Ticket PDF Generation

- **Signed PDF tickets with QR codes.** On confirmed orders, each ticket can be downloaded as a PDF containing event details, seating info, and a QR code encoding a signed JWT verification token.
- **HMAC-SHA256 signing** with a separate secret (`mockhub.ticket.signing-secret`) from auth JWTs. Ticket tokens have no expiration.
- **Verification endpoint** (`GET /api/v1/tickets/verify?token={jwt}`) is public — QR scanning at venues doesn't require authentication. The signed JWT itself proves authenticity.
- **Scan tracking:** First scan sets `scannedAt` on `OrderItem`. Subsequent scans return "already scanned" warning.
- **Services:** `TicketSigningService` (JJWT signing/verification), `QrCodeService` (ZXing in-memory QR), `TicketPdfService` (PDFBox rendering, orchestrates the pipeline).
- **All in-memory** — no temp files. PDFBox uses standard Helvetica fonts (no custom font loading).
- **Frontend:** Download icon button on `OrderItemRow` (visible when order status is CONFIRMED). Uses blob download pattern.

### Seller Flow

- **Any authenticated user can sell.** No separate seller role — `seller_id` is nullable on `listings` (NULL = platform/primary-market listing, non-null = user-created resale listing).
- **Seller creates listing by describing seat:** `POST /api/v1/listings` with eventSlug, sectionName, rowLabel, seatNumber, price. Backend finds the matching Ticket, validates availability, sets seller, creates Listing.
- **Ownership enforcement:** Update price and deactivate check `listing.seller.id == authenticated user`. Throws `UnauthorizedException` on mismatch.
- **Duplicate prevention:** `existsByTicketIdAndStatus(ticketId, "ACTIVE")` rejects re-listing an already-listed ticket.
- **Earnings aggregation:** Computed from `OrderItem` where `listing.seller.id` matches and `order.status = 'COMPLETED'`.
- **Endpoints:** `POST /api/v1/listings`, `GET /api/v1/my/listings`, `PUT /api/v1/listings/{id}/price`, `DELETE /api/v1/listings/{id}`, `GET /api/v1/my/owned-tickets`, `GET /api/v1/my/earnings`
- **Owned tickets endpoint:** `GET /api/v1/my/owned-tickets` returns tickets from confirmed orders (status `SOLD`) that the user can relist. Queries `OrderItem → Ticket` with eager joins for event/venue/section/seat data. Returns `List<OwnedTicketDto>`.
- **Frontend:** 3 pages (SellPage, MyListingsPage with tab filtering, EarningsPage dashboard), seller API + hooks, nav links visible when authenticated.
- **SellPage smart step 1:** Shows owned tickets as selectable cards above the event search. Clicking one pre-fills event/section/row/seat and skips to the price step. GA tickets without row/seat go to the seat details step instead. Falls through to the search flow when user has no owned tickets.
- **VenueMap in sell form:** Step 2 renders the `VenueMap` component when SVG section data is available. Clicking a section auto-fills the section dropdown. Row and seat remain manual text inputs.

### OAuth2 Social Login

- **Three providers:** Google, GitHub, Spotify via `spring-boot-starter-oauth2-client`.
- **Stateless OAuth2 state:** `CookieOAuth2AuthorizationRequestRepository` stores OAuth2 state in HMAC-signed JSON cookies (not server sessions). Compatible with Railway's stateless deployment.
- **Code exchange flow:** `OAuth2AuthenticationSuccessHandler` generates a one-time code, redirects to `/auth/callback?code=...`, frontend exchanges via `POST /api/v1/auth/oauth2/exchange?code=...` (query param, not JSON body).
- **Pending auth cleanup:** `@Scheduled` every 5 min removes expired one-time codes from the `pendingAuths` map. Prevents unbounded memory growth from abandoned OAuth flows.
- **User linking:** First OAuth login creates a user account. Subsequent logins from the same email link the OAuth provider to the existing account via `OAuthAccount` entity.
- **Profile page:** `/my/profile` shows edit form (name, phone) and connected OAuth providers.
- **Environment:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` (Spotify uses the same vars as the API integration). `OAUTH2_FRONTEND_REDIRECT_URL` for Railway vs localhost.
- **`forward-headers-strategy: framework`** required in production for correct OAuth redirect URIs behind Railway's TLS proxy.

### Spotify Integration

- **Three phases shipped:** Phase 1 (API metadata), Phase 2 (embedded player, genre tags), Phase 3 (user listening history for personalized recommendations).
- **Client Credentials OAuth2:** `SpotifyApiService` authenticates with Spotify's token endpoint using Basic auth (client ID + secret). Tokens cached until near-expiry.
- **User OAuth2 (Phase 3):** `SpotifyListeningApiService` uses the user's OAuth access/refresh tokens (stored encrypted) to fetch `GET /v1/me/top/artists` and `GET /v1/me/player/recently-played`. Scopes: `user-read-email,user-top-read,user-read-recently-played`.
- **Token storage:** Spotify access/refresh tokens stored encrypted (AES-256-GCM) in `oauth_accounts` via `TokenEncryptionConverter` JPA AttributeConverter. Key from `SPOTIFY_TOKEN_ENCRYPTION_KEY` env var (32-byte Base64). Random 12-byte IV per encryption.
- **Lazy token refresh:** On 401, `SpotifyListeningApiService` refreshes via `POST /api/token` with `grant_type=refresh_token`. Updates stored tokens. Falls back to stale cache on failure.
- **Listening cache:** `spotify_listening_cache` table (JSONB columns) caches top artist IDs/names, genres, recently played artist IDs per user. 24-hour TTL. Minimizes API calls under Spotify's 5-user dev mode cap.
- **Incremental consent:** `scopes_granted` column on `OAuthAccount` tracks which scopes were granted. `SpotifyListeningService` returns `scopeUpgradeNeeded=true` if `user-top-read` or `user-read-recently-played` is missing. Frontend shows "Update permissions" button.
- **Connect/disconnect:** `SpotifyConnectionService` manages connection status and disconnect (deletes tokens + listening cache). `GET/DELETE /api/v1/spotify/connection`.
- **Privacy cleanup:** `SpotifyDataCleanupScheduler` runs nightly to remove orphaned cache entries where user no longer has a Spotify OAuth account.
- **Enhanced recommendations:** `RecommendationService` injects `Optional<SpotifyListeningService>`. Spotify artist names and genres are added to the AI prompt. Events matching the user's Spotify artist IDs are included in the candidate pool even if not featured. Optional `city` query parameter filters both featured and Spotify-matched events (case-insensitive).
- **Mock implementation:** `MockSpotifyListeningService` (`mock-spotify` profile) returns hardcoded Beyonce/Drake/Radiohead data. No Spotify credentials needed for dev/testing.
- **Spotify dev mode cap:** 5 allowlisted users max, each needs Spotify Premium. Extended Quota Mode requires 250K MAU — not applicable for teaching projects. The 24-hour cache and mock profile make the feature usable within this constraint.
- **Artist ID validation:** Spotify IDs are 22-char base62 strings. Validated with `^[a-zA-Z0-9]{22}$` regex in `SpotifyApiService.getArtist()` and `@Pattern` on `EventCreateRequest.spotifyArtistId`.
- **Iframe URL encoding:** Frontend uses `encodeURIComponent()` on artist IDs in both the embed iframe `src` and "Open in Spotify" link `href`.
- **Embedded player limitation:** Playback doesn't work due to browser third-party cookie partitioning (Spotify Connect returns 403). A note below the player explains this and directs users to the "Open in Spotify" link.
- **Rate limit retry:** Exponential backoff with `Retry-After` header support. Max 3 retries.
- **Caching:** Artist metadata cached 1 hour in `ConcurrentHashMap`.
- **Profile-based:** `spotify` profile activates `SpotifyApiService` (`@Primary`) and `SpotifyListeningApiService`. Without it, mock implementations return empty/hardcoded data.

### Concurrency Control

- **Optimistic locking** on `Ticket`, `Listing`, and `Order` entities via JPA `@Version` fields (Flyway V30). Hibernate includes `WHERE version = ?` on every UPDATE and throws `OptimisticLockException` if the row changed since read.
- **`TicketService.reserveTicket()` uses `saveAndFlush()`** — forces the SQL UPDATE immediately so the version check happens inside the `try/catch` in `OrderService`, not at deferred transaction commit time. Without this, the exception escapes the catch block.
- **`OrderService.validateAndReserveTickets()`** catches `OptimisticLockingFailureException` and converts it to `ConflictException("was just purchased by another buyer")` — user-friendly 409 response.
- **`GlobalExceptionHandler`** also catches `OptimisticLockingFailureException` as a safety net for any uncaught optimistic lock failures outside the checkout flow.
- **Atomic SQL for `Event.availableTickets`** — `decrementAvailableTickets()` and `incrementAvailableTickets()` in `EventRepository` use `UPDATE ... SET available_tickets = available_tickets - 1 WHERE available_tickets > 0`. Avoids version contention when multiple different tickets for the same popular event sell concurrently. Return value is checked — logs a warning if zero rows affected.
- **Pessimistic locking on Order state transitions** — `confirmOrder()`, `failOrder()`, `cancelOrder()` use `findByOrderNumberForUpdate()` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This was pre-existing; optimistic locking on Ticket/Listing complements it.
- **Concurrency integration test** — `TicketConcurrencyIntegrationTest` uses `ExecutorService` with two threads attempting to checkout the same ticket simultaneously via Testcontainers. Verifies exactly one succeeds.

### Seed Data and Image Restoration

- **`DataSeeder`** — `@Profile("dev")` only. Seeds sample users, venues, events, and tickets. Does NOT run in production.
- **`SeedImageRestorer`** — no profile restriction, runs on every startup in all environments. Restores event images from classpath to the filesystem. Required because Railway's ephemeral filesystem loses uploaded files on every redeploy. Extracted from `EventSeeder` to separate the production-critical image restoration from dev-only sample data seeding.

### Lifecycle Cleanup

- **`LifecycleCleanupService`** — `@Scheduled` every 15 min (configurable via `mockhub.lifecycle.cleanup-interval`).
- **Four operations:** (1) Expire ACTIVE listings past `expires_at` → EXPIRED, (2) Expire ACTIVE listings for past events → EXPIRED, (3) Mark past ACTIVE events as COMPLETED, (4) Delete read notifications older than 30 days.
- **Ticket release:** When listings expire, their tickets are released from LISTED back to AVAILABLE. Uses SELECT + iterate (not bulk UPDATE) because both listing and ticket state must update together.
- **Listing → SOLD:** `OrderService.markTicketsAsSold()` also sets `listing.status = "SOLD"`.
- **Cancel re-activates:** `OrderService.releaseOrderTickets()` resets listing status back to ACTIVE alongside ticket release.
- **MCP session recovery:** `McpSessionRecoveryFilter` converts Spring AI's "Session not found" JSON-RPC errors (HTTP 200) to HTTP 404, enabling `mcp-remote` clients to detect stale sessions after Railway redeploys.

### Calendar Integration

- **RFC 5545 iCalendar** (.ics) file generation via `CalendarService`.
- **Endpoint:** `GET /api/v1/orders/{orderNumber}/calendar` — authenticated, returns `.ics` file download with event name, date/time, venue address, doors-open time, ticket count, and order number.
- **MCP tool:** `getCalendarEntry` in `OrderTools` — returns `.ics` content for agent-driven post-purchase flows.
- **Frontend:** "Add to Calendar" button on `OrderConfirmationPage` (visible for CONFIRMED orders only), triggers blob download.
- **No external dependencies** — `.ics` is a text format generated with `StringBuilder`. The OS handles import into any calendar app.

### Agentic Commerce

- **Three-layer architecture:** (1) MCP tools for agent capabilities, (2) Mandates for agent authorization, (3) ACP endpoints for protocol interoperability. See `docs/agentic-commerce.md` for full documentation.
- **`llms.txt`** — served at `/llms.txt` (static resource), describes all API endpoints, MCP tools, and ACP endpoints for AI agents.
- **RFC 9457 Problem Details** — all error responses use Spring's `ProblemDetail` format for machine-readable errors.
- **MCP server** — 23 tools registered (EventTools, PricingTools, CartTools, OrderTools, MandateTools) via `spring-ai-starter-mcp-server-webmvc`. Uses Streamable HTTP transport (protocol: `STREAMABLE`) at `/mcp`.
- **MCP auth (two modes, profile-based):**
  - `mcp-oauth2` profile: OAuth 2.1 with Dynamic Client Registration (DCR). Enables native Claude connector support on all platforms (desktop, web, mobile) without `mcp-remote`. Uses `org.springaicommunity:mcp-authorization-server:0.1.5` and `org.springaicommunity:mcp-server-security:0.1.5`.
  - Without `mcp-oauth2` profile: Falls back to `X-API-Key` header auth via `McpApiKeyFilter`. Requires `mcp-remote` bridge for Claude Desktop.
- **OAuth2 SecurityFilterChain architecture** (when `mcp-oauth2` active): Three chains with explicit `@Order` — (1) authorization server chain (OAuth2 endpoints, `/.well-known/**`, DCR), (2) MCP resource server chain (`/mcp/**`, validates Bearer tokens), (3) existing chain (everything else, unchanged). Chains are mutually exclusive with `McpApiKeyFilter` via `@Profile("!mcp-oauth2")`.
- **OAuth2 authorization server** embedded in MockHub. Ephemeral RSA key pair for JWT signing (regenerated on restart). Pre-registered Claude client with redirect URI `https://claude.ai/api/mcp/auth_callback`. DCR allows additional clients to self-register.
- **OAuth2 login page** at `/oauth2/login` — form for authorization_code flow. Separate from React SPA login. Uses MockHub's existing `UserDetailsServiceImpl`. After clicking "Authorize", shows "Authorizing..." then transitions to an "Authorization Complete" card with a checkmark, since the OAuth2 redirect chain sends the browser to the client callback URL and may not provide its own feedback.
- **Environment variables:** `MCP_OAUTH2_ISSUER_URI` (must match public URL in production, e.g., `https://mockhub.kousenit.com`).
- **SPA exclusions:** `oauth2/`, `.well-known/` added to `SpaForwardingConfig`.
- **MCP tools identify users by email** — cart and order tools accept `userEmail` parameter, not auth tokens.
- **Complete agent purchase flow:** `findTickets` → `addToCart` → `checkout` → `confirmOrder` — agents can now execute full purchases.
- **`findTickets` compound tool** — single-call search with query, category, city, date range, price range, section filter, returning matching listings sorted by price. Uses JPA `Specification` with `findBy` fluent API (no `COUNT` query overhead). `ListingSearchCriteria` record encapsulates all filters; `ListingSearchSpecification` builds predicates dynamically for non-null criteria only. Date parameters are `String` (not `Instant`) because Spring AI MCP can't deserialize ISO-8601 to `Instant`. Reduces agent round-trips from 3 to 1. Server-side execution: ~54ms.
- **`getEventListings` paginated** — accepts `page`/`size` params (default 20, max 50), returns `{ listings, page, size, totalListings }`. Without pagination, popular events (500+ listings) exceeded MCP context limits.
- **`getCalendarEntry` tool** — returns RFC 5545 .ics content for a confirmed order.
- **`SpendingLimitCondition`** — WARNING eval condition when cart exceeds configurable limit (`mockhub.eval.max-cart-total`, default $2000).

### Agent Mandates

- **Authorization model for agents.** A `Mandate` record defines what an agent can do on behalf of a user: scope (BROWSE/PURCHASE), spending limits (per-transaction + cumulative), category/event restrictions, expiration.
- **`MandateCondition`** — CRITICAL eval condition that blocks agent actions without a valid mandate. Checks scope, spending limits, and category/event constraints.
- **MandateTools** — 5 MCP tools for mandate lifecycle: `createMandate`, `revokeMandate`, `listMandates`, `validateMandate`, `getBestMandate` (compound lookup, recommended before `addToCart`).
- **Mandate entity** in `com.mockhub.mandate` package. Flyway migration `V22__create_mandates_table.sql`.
- **Inspired by AP2** (Google's Agent Payments Protocol) — mandates are MockHub's implementation of AP2's digitally signed authorization concept, enforced through the eval conditions framework.
- **PURCHASE scope subsumes BROWSE** — an agent authorized to buy can also browse.

### ACP (Agentic Commerce Protocol)

- **ACP-compatible endpoints** at `/acp/v1/` — RESTful checkout API compatible with the Stripe+OpenAI Agentic Commerce Protocol.
- **Six endpoints:** `POST /checkout` (create), `GET /checkout/{id}` (status), `PUT /checkout/{id}` (update), `POST /checkout/{id}/complete`, `POST /checkout/{id}/cancel`, `GET /catalog` (product discovery).
- **Pure adapter layer** — wraps existing CartService + OrderService. No business logic was modified.
- **API key auth** via `AcpApiKeyFilter` — same `mockhub.mcp.api-key` property as MCP, independent filter.
- **SPA exclusion** — `/acp/**` added to `SpaForwardingConfig` exclusion list.

### Frontend

- **TypeScript** — no `any` types. Use proper interfaces defined in `src/types/`.
- **React Query (TanStack)** for all server state. Zustand only for client-side UI state (auth token, cart drawer, mobile nav).
- **API layer:** `src/api/*.ts` files export typed functions. `src/hooks/use*.ts` files wrap them in React Query hooks. Components use hooks, never call API functions directly.
- **shadcn/ui components** live in `src/components/ui/` (18 components including Tooltip). Custom components live in feature folders (`events/`, `cart/`, `checkout/`, `sell/`, etc.).
- **`TooltipProvider`** wraps the app in `App.tsx` (required by Radix Tooltip). Must also be included in `test-utils.tsx` `renderWithProviders` wrapper — any new provider added to `App.tsx` must be mirrored there.
- **Tooltips on disabled elements:** Disabled buttons swallow pointer events. Wrap with `<span className="inline-flex" tabIndex={0}>` to restore hover/focus for the tooltip trigger (see `FavoriteButton.tsx` for the pattern).
- **Mobile-first** responsive design using Tailwind breakpoints.
- **No inline styles.** Use Tailwind utility classes exclusively.
- **Error responses:** Frontend `ApiError` type uses `detail` field (RFC 9457), not `message`.

### Testing

- **Every feature must have tests.** This is non-negotiable.
- **Tests MUST be written before pushing.** Do not rely on SonarCloud to catch coverage gaps. SonarCloud enforces 80% coverage on new code — every untested new method tanks the metric. Write the test immediately after (or before) writing the production method. Run `./gradlew test jacocoTestReport` locally and verify coverage on new files before creating a PR. Repeated quality gate failures from missing tests are unacceptable.
- Backend unit tests: JUnit 5 + Mockito, naming convention `methodName_givenCondition_expectedResult`
- Backend controller tests: MockMvc + @WebMvcTest
- Backend integration tests: Testcontainers with real PostgreSQL
- Frontend component tests: Vitest + React Testing Library, colocated as `*.test.tsx`
- Frontend API mocking: MSW (Mock Service Worker)
- **jsdom polyfills:** `ResizeObserver` stub is in `src/test/setup.ts` — required for any Radix UI component that uses poppers/tooltips. Add other browser API stubs here as needed.
- **Tooltip testing:** Use `findByRole('tooltip')` not `findByText` — Radix renders tooltip content in two DOM locations (visible + accessible hidden), causing `findByText` to fail with duplicate matches.
- E2E tests: Playwright configured for Chrome, Safari, Mobile iOS (3 browsers covering all rendering engines). Sharded across 2 CI jobs for faster runs.
- E2E accessibility checks: axe-core via `@axe-core/playwright`

### Code Style

- **Backend:** Follow standard Java conventions. Indentation: 4 spaces. Max line length: 120 characters.
- **Frontend:** ESLint + Prettier enforced. Indentation: 2 spaces. Single quotes. Trailing commas.
- **Comments:** Only where logic isn't self-evident. No boilerplate javadoc on obvious methods.
- **Naming:**
  - Java: `PascalCase` for classes, `camelCase` for methods/variables, `SCREAMING_SNAKE_CASE` for constants
  - TypeScript: `PascalCase` for components/types, `camelCase` for functions/variables, `kebab-case` for file names
  - Database: `snake_case` for tables and columns
  - API paths: `kebab-case` (`/api/v1/price-history`)

### Git Conventions

- Feature branches: `feature/<description>`
- Commit messages: imperative mood, concise ("Add event search filtering", not "Added event search filtering functionality")
- One logical change per commit
- Use TDD on feature branches: write tests first (RED), implement (GREEN), refactor

## CI / Quality

- **GitHub Actions** (`.github/workflows/ci.yml`) runs on push to main and PRs: backend tests, frontend lint/typecheck/tests, SonarCloud analysis, Docker build smoke test
- **SonarCloud** — org: `kousen-it-inc`, project: `kousen_mockhub`. All config is in `sonar-project.properties` at the repo root (both backend and frontend). CI uses `SonarSource/sonarqube-scan-action` (not the Gradle plugin). Token stored as `SONAR_TOKEN` GitHub secret. Both JaCoCo (backend) and lcov (frontend) coverage feed the quality gate.
- **Issue exclusions** are in `sonar-project.properties` (16 rules). Key suppressions: S1186 (JPA empty constructors), S1192 (seed data/SQL literals), S3776 (seed data complexity), S6218 (byte[] record), S6863 (verification endpoint), S7746 (Axios throw style), S107 (PDFBox parameter count), S6845 (tooltip tabIndex), S6395 (regex grouping), S125 (regex doc comment), S7764 (window vs globalThis), S7748 (mock data zero fractions), S1075 (filter hardcoded URIs), S7735 (negated ternaries), S135 (sync loop breaks). See `sonar-project.properties` for the complete list with justifications.
- **SonarQube MCP server** is available in this project. Use it to query SonarCloud for issues, quality gate status, and hotspots. When fixing code, check SonarCloud issues first.
- **GitHub repo:** `kousen/mockhub` (public, MIT license)

## Deployment

- **Platform:** Railway (Hobby plan, $5/mo minimum)
- **URL:** https://mockhub.kousenit.com
- **Architecture:** Single Docker container serves both Spring Boot API and React SPA (no CORS needed)
- **Dockerfile:** Root `Dockerfile` builds frontend (Node), bundles into Spring Boot jar, runs on JRE Alpine
- **SPA routing:** `SpaForwardingConfig` serves `index.html` for client-side routes, excludes `api/`, `actuator/`, `mcp/`, `acp/`, `swagger-ui/`, `v3/` paths
- **Security:** Static frontend routes (`/`, `/events/**`, `/sell`, `/my/**`, etc.) are `permitAll()` in SecurityConfig. CORS allows the Railway production domain.
- **Ephemeral filesystem:** Seed images are restored from classpath on every container startup via `SeedImageRestorer` (runs in all profiles). `DataSeeder` is dev-only — does NOT run in production.
- **Graceful shutdown:** `server.shutdown=graceful` with `spring.lifecycle.timeout-per-shutdown-phase=30s` in `application-prod.yml`. In-flight requests (especially checkout/payment) complete before the container stops during Railway redeploys.
- **Profiles:** `prod,ai-anthropic,mock-payment,sms-twilio,email-resend,spotify,ticketmaster,mcp-oauth2` — production datasource, Anthropic AI, mock payment, real SMS and email, Spotify, Ticketmaster, MCP OAuth2
- **Database:** Railway PostgreSQL with separate `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` env vars (Railway's `DATABASE_URL` format is incompatible with JDBC)
- **JWT secret:** Must be valid Base64 (no dots or special characters)
- **Auto-deploy:** Pushes to `main` trigger automatic Railway deployments

## File Reference

- `ARCHITECTURE.md` — Database schema, API design, backend/frontend architecture, key decisions
- `PROJECT_JOURNAL.md` — Build report with session notes, challenges, metrics, and commit history
- `docs/demo-transcript-agentic-purchase.md` — Full agentic purchase demo transcript (Claude Desktop + MCP, 2026-03-26)
- `docs/stripe-test-setup.md` — Stripe test mode API key setup instructions
- `sonar-project.properties` — SonarCloud configuration for frontend (coverage exclusions, issue suppressions)
- `backend/build.gradle.kts` — Backend build config (dependencies, test setup, code style)
- `.github/workflows/ci.yml` — CI pipeline (backend tests incl. Testcontainers, frontend lint/typecheck/tests, SonarCloud, Docker build)
- `Dockerfile` — Combined frontend+backend Docker build for production deployment
- `docker-compose.yml` — Full stack (Postgres, backend, frontend)
- `docker-compose.dev.yml` — Postgres only (for local development)
- `backend/src/main/resources/application-prod.yml` — Production profile (Railway datasource, PORT binding)
- `backend/src/main/resources/static/llms.txt` — API description for AI agent discovery
- `docs/evaluation-conditions.md` — Eval conditions concept, Design by Contract mapping, architecture, how to add conditions
- `docs/agentic-commerce.md` — Agentic commerce architecture: MCP tools, mandates, ACP endpoints, protocol landscape, teaching connections

## What NOT to Do

- Do not use `var` in Java (use explicit types for readability — this is a teaching codebase)
- Do not add dependencies without justification — check `build.gradle.kts` for what's already included
- Do not use `localStorage` for JWT tokens (security anti-pattern — use in-memory + HttpOnly refresh cookie)
- Do not write Flyway migrations with `ddl-auto: create` or `update` (always `validate`)
- Do not create REST endpoints that bypass the service layer
- Do not suppress exceptions silently — log or rethrow as domain exceptions
- Do not use `@Autowired` on fields — use constructor injection (implicit with single constructor)
- Do not seal interfaces that need Mockito mocking in `@WebMvcTest` tests (Mockito can't create subclasses of sealed types)
- Do not add SonarCloud issue exclusions anywhere other than `sonar-project.properties` at the repo root
