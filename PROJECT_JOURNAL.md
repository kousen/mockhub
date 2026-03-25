# MockHub Build Report

## Project Summary

MockHub is a secondary concert ticket marketplace (like StubHub) built as a teaching platform for undergraduate AI students. The entire application was constructed using Claude Code's parallel agent system across 5 waves, with 11 agents working in isolated git worktrees.

**Repository:** `kousen/mockhub` (public, MIT license)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4, Java 25, Gradle 9.4.0 (Kotlin DSL) |
| Database | PostgreSQL 17 + pgvector, H2 (dev profile) |
| AI | Spring AI 2.0.0-M3 (Anthropic, OpenAI, Ollama profiles) |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui |
| State Management | TanStack React Query (server), Zustand (client) |
| Payments | Stripe test mode + mock fallback via Spring profiles |
| Testing | JUnit 5, Mockito, Testcontainers, Vitest, React Testing Library, MSW, Playwright |
| Infrastructure | Docker Compose, nginx, GitHub Actions CI, SonarCloud |

---

## Build Process

### Wave Architecture

The build was organized into 5 sequential waves. Within each wave, agents ran in parallel using isolated git worktrees, then merged back to `main` before the next wave started.

```
Wave 1 (3 agents) → Wave 2 (2 agents) → Wave 3 (2 agents) → Wave 4 (2 agents) → Wave 5 (3 agents)
```

### Wave 1 — Foundation (3 agents)

| Agent | Deliverables |
|-------|-------------|
| **backend-foundation** | Spring Boot init, Gradle config, 15 Flyway migrations (V0–V14), BaseEntity, auth (User/Role, JWT, SecurityConfig), GlobalExceptionHandler, Spring AI config, all application YAML profiles |
| **frontend-foundation** | Vite + React + TypeScript init, Tailwind/shadcn setup, Axios client with JWT interceptors, auth store (Zustand), MainLayout/Header/Footer, Login/Register pages, ProtectedRoute/AdminRoute |
| **infrastructure** | docker-compose.yml (full stack), docker-compose.dev.yml (Postgres only), backend/frontend Dockerfiles, nginx.conf, .env.example files, GitHub Actions CI, SonarCloud config |

**Result:** Running app with registration, login, placeholder pages, Swagger UI.

### Wave 2 — Catalog (2 agents)

| Agent | Deliverables |
|-------|-------------|
| **backend-catalog** | Venue/Section/SeatRow/Seat entities, Event/Category/Tag entities, EventService with search/filter/pagination (Spring Data Specifications), SearchService (PostgreSQL tsvector), PricingEngine with @Scheduled job, TicketService, ListingService, PriceHistoryService, all controllers |
| **frontend-catalog** | HomePage (featured events, categories), EventListPage (search, filter, sort, paginate), EventDetailPage, EventCard, EventGrid, EventSearch, EventFilters, CategoryNav, TicketListView, PriceTag, price history chart, all hooks |

**Result:** Users can browse, search, and filter events. Event detail pages show ticket listings with dynamic prices and price history.

### Wave 3 — Commerce (2 agents)

| Agent | Deliverables |
|-------|-------------|
| **backend-commerce** | Cart/CartItem entities, Order/OrderItem entities, CartService (add/remove/clear/expiration), OrderService (checkout flow, ticket reservation), PaymentService interface + MockPaymentService + StripePaymentService, TransactionLog, all controllers, ticket status transitions (AVAILABLE → RESERVED → SOLD) |
| **frontend-commerce** | CartDrawer, CartItem, CartSummary, CartPage, cart store (Zustand), CheckoutPage, OrderReview, MockPaymentForm, StripePaymentForm, OrderConfirmationPage, OrderHistoryPage, useCart/useOrders hooks with optimistic updates, "Add to Cart" on ticket listings |

**Result:** Full purchase flow from adding tickets to cart through payment to order confirmation.

### Wave 4 — Engagement & Admin (2 agents)

| Agent | Deliverables |
|-------|-------------|
| **backend-engagement-admin** | Favorite entity/repo/service/controller, Notification entity/repo/service/controller, NotificationService wired into OrderService (order confirmations), scheduled event reminder job (24h before), AdminService (dashboard stats), AdminController (all admin endpoints), event creation with ticket generation, user management |
| **frontend-engagement-admin** | FavoriteButton (heart toggle on EventCards), FavoritesPage, NotificationBell (header, unread count, dropdown with polling), AdminLayout (sidebar nav), AdminDashboardPage (stats cards), AdminEventsPage (data table), AdminEventFormPage (create/edit), AdminUsersPage (data table), useFavorites/useNotifications/useAdmin hooks |

**Result:** Favorites, notifications, order history, and full admin dashboard.

### Wave 5 — Polish & Test (3 agents)

| Agent | Deliverables |
|-------|-------------|
| **seed-data-images** | ImageStorageService + LocalImageStorageService, ImageResizer, ImageController, DataSeeder orchestrator (dev profile only), UserSeeder (8 users), VenueSeeder (22 venues), EventSeeder (100+ events), TicketSeeder (tickets + listings), OpenAPI annotations on all 15 controllers and key DTOs |
| **frontend-polish** | EmptyState component, ErrorBoundary, PriceDisplay component, responsive design fixes (mobile nav active states, admin link, notification bell), Skeleton loading states for all pages, lazy image loading, EventCard line-clamp |
| **testing** | 22 backend test files (11 service unit tests, 6 controller tests, 5 integration tests with Testcontainers), 5 frontend component tests (Vitest + React Testing Library), 4 Playwright E2E specs (5 browsers: Chrome, Firefox, Safari, Mobile Android, Mobile iOS), MSW handlers, test utilities |

**Result:** Polished, responsive app with realistic seed data, complete API docs, and full test suite.

---

## Challenges During Build

### API Overload (529 Errors)

Multiple agents encountered Anthropic API overload errors during Waves 4 and 5. The recovery process involved:

1. Checking each agent's worktree for committed and uncommitted work
2. Resuming agents via `SendMessage` (which failed when the agent process had died)
3. Relaunching fresh agents with context about what was already completed
4. In one case (Wave 4 frontend), finishing the last 2 file edits directly rather than relaunching

### Merge Complexity (Wave 3 Frontend)

The `feature/frontend-commerce` branch diverged from before Wave 1, causing the diff to show all backend files as "deleted." A naive merge would have reverted backend work. Resolution required:

1. Identifying that only 25 frontend files actually differed
2. Performing the merge and resolving 6 conflicts manually
3. Taking the commerce additions while preserving the existing Wave 1/2 content

---

## Codebase Metrics (as of 2026-03-18)

### File and Line Counts

| Category | Files | Lines |
|----------|------:|------:|
| Backend Java (production) | 140 | 9,166 |
| Backend Java (tests) | 33 | 4,874 |
| Frontend TS/TSX (production) | 105 | 7,740 |
| Frontend tests (unit + E2E) | 11 | 747 |
| Flyway migrations | 16 (V0–V15) | — |
| Seed images | 25 | 1.9 MB |
| **Total** | **~330** | **~22,500** |

### Backend Architecture

| Layer | Count | Examples |
|-------|------:|---------|
| Controllers | 15 | Auth, Event, Venue, Cart, Order, Payment, Favorite, Notification, Admin, Search, Price, Category, Tag, AI, Image |
| Services | 21 | AuthService, EventService, CartService, OrderService, PricingEngine, PricingUpdateService, NotificationService, AdminService, MockPaymentService, StripePaymentService, etc. |
| Entities | 23 | User, Role, Venue, Section, SeatRow, Seat, Event, Category, Tag, Ticket, Listing, Cart, CartItem, Order, OrderItem, Favorite, Notification, Image, TransactionLog, PriceHistory |
| DTOs (records) | 39 | Request/Response records with Jakarta validation |
| Repositories | 17 | Spring Data JPA interfaces |
| Feature packages | 17 | admin, ai, auth, cart, common, config, event, favorite, image, notification, order, payment, pricing, search, seed, ticket, venue |

### Frontend Architecture

| Layer | Count |
|-------|------:|
| Pages | 17 |
| Components | 50 |
| API modules | 12 |
| React Query hooks | 9 |
| Zustand stores | 3 |
| Type definitions | 10 |

### Test Suite

| Test Type | Files | Tests | Description |
|-----------|------:|------:|-------------|
| Backend service unit tests | 15 | ~119 | JUnit 5 + Mockito |
| Backend controller tests | 10 | ~51 | MockMvc + @WebMvcTest |
| Backend integration tests | 8 | ~30 | Testcontainers PostgreSQL |
| Frontend unit tests | 7 | 38 | Vitest + React Testing Library + MSW |
| Playwright E2E | 4 | 91 | 5 browsers: Chrome, Firefox, Safari, Mobile Android, Mobile iOS |
| **Total** | **44** | **~329** | |

### Quality Metrics (SonarCloud)

| Metric | Value |
|--------|-------|
| Coverage | 69.9% (on 1.8k lines to cover) |
| Duplications | 0.4% |
| Maintainability | A |
| Security hotspots | 0 (all reviewed) |

### Seed Data

| Entity | Count |
|--------|-------|
| Users | 8 (admin, buyer, seller, 5 random) |
| Venues | 22 (arenas, theaters, amphitheaters, clubs, stadiums) |
| Events | 80+ (concerts, sports, theater, comedy, festivals) |
| Tickets/Listings | Generated per event (~35% of seats listed) |
| Images | 25 curated photos from Unsplash (5 per category) |

### API Endpoints

| Group | Endpoints |
|-------|-----------|
| Auth | 4 (register, login, refresh, me) |
| Events | 7 (list, detail, search, create, update, featured, sections) |
| Venues | 3 (list, detail, sections) |
| Cart | 4 (get, add, remove, clear) |
| Orders | 3 (checkout, list, detail) |
| Payments | 3 (create intent, confirm, webhook) |
| Favorites | 4 (list, add, remove, check) |
| Notifications | 4 (list, unread count, mark read, mark all read) |
| Admin | 10 (dashboard, event CRUD, user management, orders, ticket generation) |
| Search | 2 (search, suggest) |
| Pricing | 1 (price history) |
| Images | 3 (serve by ID, thumbnail, serve by filename) |
| AI (stubs) | 4 (recommendations, NL search, chat, price prediction) |
| **Total** | **~50** |

---

## Commit History

```
b16e34a Fix security filter chain and integration tests: all 148 tests pass
3d04cf4 Rename BUILD_REPORT.md to PROJECT_JOURNAL.md
b613dd8 Fix test suite for Spring Boot 4: dependencies, imports, and AI config
7a70ad3 Add build report documenting the 5-wave agent build process
0e67b25 Update Anthropic model to claude-sonnet-4-6
54688ff Merge Wave 5: seed data, images, and OpenAPI documentation
873f837 Add image storage, seed data, and OpenAPI documentation (Wave 5)
a64fe61 Add comprehensive test suite: unit, integration, component, and E2E tests (Wave 5)
ee7d836 Add responsive polish, empty states, error boundary, and loading skeletons (Wave 5)
bf76020 Merge Wave 4: frontend engagement and admin (favorites, notifications, admin UI)
d709261 Add favorites, notifications, and admin dashboard UI (Wave 4 frontend)
eccacea Add favorites, notifications, and admin dashboard (Wave 4 backend)
b0f33fb Merge branch 'feature/frontend-commerce'
6a5bd8c Merge Wave 3: backend commerce (cart, checkout, orders, payments)
54790f1 Add commerce UI: cart drawer, checkout, orders, payment forms
fd8ef83 Add cart, order, and payment processing (Wave 3 commerce)
505f4cd Merge Wave 2: frontend catalog (browse, search, filters, price charts)
54717c3 Merge Wave 2: backend catalog (venues, events, tickets, pricing, search)
4447f05 Add catalog browsing UI: events, search, filters, tickets, price charts
d352bf7 Add catalog and marketplace: venues, events, tickets, pricing, search
1b0415e Add worktrees and Eclipse IDE files to .gitignore
45d0e0f Remove Eclipse IDE files from backend
3f85f0b Merge Wave 1: frontend foundation (React, auth UI, layout, routing)
cdea1a6 Merge Wave 1: backend foundation (Spring Boot, auth, migrations, Spring AI)
d94cb1c Merge Wave 1: infrastructure (Docker, Dockerfiles, nginx, env examples)
cb0bbb3 Add React frontend foundation: auth, layout, routing, Tailwind/shadcn
0953988 Add Spring Boot backend foundation: auth, migrations, Spring AI config
a25f49c Add Docker infrastructure: Compose files, Dockerfiles, nginx, env examples
4f28653 Update CLAUDE.md with CI, SonarCloud, and build wave instructions
0af6879 Add GitHub Actions CI and SonarCloud integration
32447f0 Add linting, accessibility testing, and 5-browser Playwright config
0d427fb Add project setup: CLAUDE.md, README, license, and community docs
2c04e61 Add Spring AI, PGVector, updated versions, and Mermaid diagrams
88d2409 Initial commit: architecture plan and gitignore
```

---

## Test Execution: Lessons Learned

The testing agent (Wave 5) generated all test files but did not execute them. Running the tests revealed significant gaps between the generated code and Spring Boot 4's actual module structure. Here are the key issues discovered and resolved:

### Spring Boot 4 Module Restructuring

Spring Boot 4.0 split the monolithic `spring-boot-starter-web` and `spring-boot-starter-test` into fine-grained modules. The generated code used Spring Boot 3 conventions, which required these dependency corrections:

| What changed | Spring Boot 3 | Spring Boot 4 |
|---|---|---|
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `@WebMvcTest` | `spring-boot-starter-test` (included) | `spring-boot-starter-webmvc-test` (separate) |
| `TestRestTemplate` | `spring-boot-starter-test` (included) | `spring-boot-resttestclient` (separate, needs `@AutoConfigureTestRestTemplate`) |
| Security test | `spring-security-test` | `spring-boot-starter-security-test` |
| JPA test | `spring-boot-starter-test` (included) | `spring-boot-starter-data-jpa-test` (separate) |
| Flyway | `flyway-core` (auto-configured) | `spring-boot-starter-flyway` (separate starter required) |
| Testcontainers | `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| Testcontainers JUnit | `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |

### Package Relocations

| Class | Spring Boot 3 package | Spring Boot 4 package |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `TestRestTemplate` | `org.springframework.boot.test.web.client` | `org.springframework.boot.resttestclient` |
| `@MockitoBean` | `org.springframework.boot.test.mock.mockito` | `org.springframework.test.context.bean.override.mockito` |

### Spring AI Multi-Provider Bean Conflict

When multiple Spring AI providers (Anthropic, OpenAI, Ollama) are all on the classpath, Spring creates a `ChatModel` bean for each. Without a `@Primary` designation, any code requesting a `ChatModel` fails with `NoUniqueBeanDefinitionException`. Resolution: added `@Primary` `ChatModel` bean in `AiConfig` and `@ConditionalOnBean` guards on AI-dependent services.

### Testcontainers PostgreSQL + pgvector

The Flyway migration V0 creates the `vector` extension and V14 uses the `vector` column type. Integration tests using plain `postgresql:17` fail on these migrations. Resolution: switched from Testcontainers JDBC URL approach to programmatic container with `pgvector/pgvector:pg17` Docker image and `@DynamicPropertySource`.

### Shared Singleton Testcontainers Pattern

Using `@Container` with `@Testcontainers` creates a per-class container lifecycle. When multiple integration test classes share the same `@SpringBootTest` context (cached by Spring), the second class starts a new container on a different port, but the cached Spring context still points to the first container's port. This causes all tests in the second class to fail with connection errors.

Resolution: replace `@Container`/`@Testcontainers` with a static initializer block that starts a single shared container:

```java
static final PostgreSQLContainer<?> POSTGRES;
static {
    POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("mockhub");
    POSTGRES.start();
}
```

This ensures all integration test classes share the same container instance for the JVM's lifetime.

### AI Auto-Configuration in Tests

Integration tests don't need AI features. The Spring AI auto-configurations must be explicitly excluded using `spring.autoconfigure.exclude` in `@SpringBootTest(properties = {...})` since property-based disabling (`spring.ai.openai.enabled=false`) is not supported by Spring AI 2.0.0-M3. The actual auto-configuration class names differ from what you might guess:
- `org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration`
- `org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration`
- `org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration`

### Security Filter Chain: Three Interleaved Bugs

The integration tests exposed three interleaved security issues that each masked the others:

**Bug 1: `/api/v1/auth/**` permitAll was too broad.** The wildcard included `/api/v1/auth/me`, which should require authentication. When an unauthenticated user hit `/me`, the controller received a null `@AuthenticationPrincipal`, threw a `NullPointerException`, and returned 500 instead of 401.

Fix: replace `.requestMatchers("/api/v1/auth/**").permitAll()` with explicit paths:
```java
.requestMatchers("/api/v1/auth/login").permitAll()
.requestMatchers("/api/v1/auth/register").permitAll()
.requestMatchers("/api/v1/auth/refresh").permitAll()
```

**Bug 2: No `AuthenticationEntryPoint` configured.** Without one, Spring Security's stateless session mode didn't produce clean 401 responses for unauthenticated API requests.

Fix: add `.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))`.

**Bug 3: `/error` endpoint not permitted.** This was the most subtle. When Spring Security correctly returned 403 (authenticated but not admin), Spring Boot's error handling forwarded the request to `/error`. But `/error` itself was protected by `.anyRequest().authenticated()`, triggering a second authentication check. Since the error-forwarded request didn't carry the original Authorization header, it was treated as unauthenticated → 401. So the client saw 401 instead of the original 403.

Fix: add `.requestMatchers("/error").permitAll()`.

This is a well-known Spring Security gotcha, but it's especially confusing when multiple bugs interact: Bug 1 caused 500s, Bug 2 caused missing 401s, and Bug 3 turned valid 403s into spurious 401s. Each fix revealed the next bug.

### Test Results: All Passing (as of 2026-03-18)

| Category | Total | Passing |
|----------|-------|---------|
| Backend unit tests (Mockito) | 119 | 119 |
| Backend controller tests (MockMvc) | 51 | 51 |
| Backend integration tests (Testcontainers) | 30 | 30 |
| Frontend component tests (Vitest) | 38 | 38 |
| Playwright E2E (5 browsers) | 91 | 91 |
| **Total** | **329** | **329** |

### Key Takeaways

1. **Never assume generated tests will pass.** The testing agent wrote syntactically valid tests with correct logic, but used Spring Boot 3 conventions for imports, dependencies, and annotations. Spring Boot 4's modularization is the most significant breaking change for test code — every test dependency was restructured. The Spring Initializr is the authoritative source for correct dependency coordinates.

2. **Running tests found real bugs.** The security filter chain issues (permitAll too broad, missing entry point, unprotected `/error`) were production bugs, not just test problems. The tests did exactly what they should — they caught bugs that would have affected real users.

3. **Test isolation matters.** The shared Testcontainers singleton pattern is essential when multiple integration test classes share a `@SpringBootTest` context. Per-class containers cause port mismatches with Spring's context caching.

---

## Agent Statistics

| Wave | Agents | Total Tool Uses | Notes |
|------|--------|----------------|-------|
| 1 | 3 | ~300+ | Pre-built before this session |
| 2 | 2 | ~250+ | Pre-built before this session |
| 3 | 2 | ~200+ | Backend pre-built; frontend merged this session with 6 conflict resolutions |
| 4 | 2 | ~206 | Both hit 529 errors; backend relaunched (49 tool uses to finish), frontend finished manually |
| 5 | 3 | ~390 | seed-data (171), frontend-polish (75), testing (144); all completed successfully |

**Total agents:** 11 across 5 waves (+ recovery relaunches)

---

---

## Session: 2026-03-18 — CI, Quality, and E2E

### CI Integration Tests Unblocked

The CI pipeline (`.github/workflows/ci.yml`) had been filtering backend tests to only run unit tests (`--tests "com.mockhub.*.service.*" --tests "com.mockhub.*.controller.*"`), which meant Testcontainers integration tests never ran on GitHub Actions. Removed the filters so CI runs all tests. This immediately exposed a Spring context startup failure on CI caused by missing OpenAI AI auto-configuration exclusions (`OpenAiAudioSpeechAutoConfiguration`, `OpenAiImageAutoConfiguration`, etc.) — locally these worked because `OPENAI_API_KEY` was set as an environment variable.

### Profile Consolidation

Merged the `dev` and `docker` Spring profiles into a single `dev` profile. Both had been using PostgreSQL via Docker since H2 was removed for Flyway compatibility. The consolidated `dev` profile uses env-var overridable database settings (`DB_HOST`, `DB_PORT`, etc.) and defaults to `mock-payment`. Stripe is activated by adding the `stripe` profile: `SPRING_PROFILES_ACTIVE=dev,stripe`.

### SonarCloud Quality Cleanup

Resolved all 5 security hotspots:
- **CSRF disabled** — safe (stateless JWT API)
- **`Random` in seeders** — safe (not security-sensitive)
- **Cookie without `secure` flag** — fixed: extracted to configurable `mockhub.cookie.secure` property
- **Missing `verification-metadata.xml`** — accepted risk for teaching project

Fixed all open issues:
- **S6809 (4 instances)** — Transactional self-invocation. CartService: made internal method private. NotificationService: inlined notification creation. PriceHistoryService: extracted shared mapping helper. PricingEngine: extracted `PricingUpdateService` for per-event transaction isolation.
- **S1192** — Extracted duplicated string literals to constants in StripePaymentService, LocalImageStorageService, ListingService, TicketService, EventService.
- **S5145** — Sanitized user-controlled data before logging in AdminService and LocalImageStorageService.
- **S5850** — Fixed regex precedence in SlugUtil, EventSeeder, VenueSeeder (`^-|-$` → `(^-|-$)`).

### Test Coverage Improvement

Added 63 new backend tests across 8 files:
- Service tests: MockPaymentService (7), PricingUpdateService (7), SearchService (12), PriceHistoryService (6)
- Controller tests: NotificationController (8), PaymentController (9), ImageController (6), SearchController (8)

Added 12 new frontend tests:
- `auth-store.test.ts` (6) — Zustand store behavior and sessionStorage persistence
- `client.test.ts` (6) — Axios interceptor token attachment, refresh-and-retry, logout on failure

Configured `sonar.coverage.exclusions` in Gradle sonar config to exclude seed data, entities, DTOs, and AiConfig from coverage calculations. Coverage rose from 44.7% to **70.4%** (on 1.8k lines to cover, down from 2.9k).

**Lesson learned:** JaCoCo exclusions and SonarCloud exclusions serve different purposes. Excluding classes from JaCoCo's `classDirectories` removes them from the coverage *report*, but SonarCloud still sees those source files and counts them as 0% covered — making coverage *worse*. The correct approach: let JaCoCo report everything, and use `sonar.coverage.exclusions` to tell SonarCloud which files to skip.

### Playwright E2E Tests Fixed (91/91)

The existing Playwright specs had selectors written against assumed DOM structure that didn't match the actual frontend. Running the tests exposed both selector mismatches and real accessibility bugs:

**Selector fixes:**
- Scoped `MockHub` and `Events` locators to `getByRole('banner')` to avoid strict mode violations from multiple matches across header/hero/footer
- Used `{ exact: true }` for `getByLabel('Password')` on register page (was matching both "Password" and "Confirm password")
- Added `test.skip(!!isMobile, ...)` for desktop nav tests that can't work on mobile viewports

**Accessibility bugs found and fixed:**
- Sort dropdown `SelectTrigger` missing accessible name → added `aria-label="Sort by"`
- Pagination disabled state using `opacity-50` caused insufficient color contrast (3.72:1 vs required 4.5:1) → replaced with `text-muted-foreground` class
- `CategoryNav` horizontal scroll container not keyboard accessible → added `role="toolbar"`, `tabIndex={0}`, `aria-label`
- Pagination `<a>` elements without `href` had prohibited `aria-label` → added `role="button"` to `PaginationLink`

### Stripe Test Keys Configured

Created `backend/.env` and `frontend/.env` (both gitignored) with Stripe test mode API keys. Documented setup process in `docs/stripe-test-setup.md` including Stripe's new Sandbox vs classic Test Mode choice.

### Commits (2026-03-18)

```
7878792 Fix Playwright E2E tests and accessibility issues (91/91 passing)
9e99067 Fix SonarCloud coverage exclusions, security and reliability issues
1dbfc69 Improve test coverage and exclude noise from coverage metrics
dd897bb Extract duplicated string literals into constants (S1192)
567c77f Fix transactional self-invocation issues (S6809)
82736c7 Make refresh cookie secure flag configurable via property
0cf032f Consolidate dev and docker profiles into single dev profile
30e492e Fix CI: exclude all OpenAI AI auto-configs and add Vitest global types
f4f2d4e Unblock CI integration tests and add missing test coverage
```

---

---

## Session: 2026-03-20 — Seller Flow (Two-Sided Marketplace)

### Overview

Added the seller flow (issue #28), turning MockHub from a buyer-only marketplace into a two-sided platform. Any authenticated user can now list tickets for sale, manage their listings, and view earnings.

### Architecture Decisions

**Seller identity as nullable FK:** Added `seller_id` to the `listings` table as a nullable FK to `users`. `NULL` = platform/primary-market listing (seeded inventory), non-null = user-created resale listing. This avoids a Flyway-vs-seeder ordering problem (migrations run before Spring seeders, so users don't exist when the migration executes) and creates a clean primary vs. secondary market distinction.

**No separate seller role:** Per issue #28, any authenticated user can sell. No `ROLE_SELLER` needed. This simplifies auth and matches the StubHub model.

**Seller creates listing by describing seat:** Rather than requiring sellers to know internal ticket IDs, they provide eventSlug + sectionName + rowLabel + seatNumber + price. The backend finds the matching Ticket via a JPQL join query across event → section → seat row → seat, validates availability, and creates the Listing + sets ticket status to LISTED. This is how real secondary marketplaces work.

**Ownership-based authorization:** Seller mutations (update price, deactivate) verify `listing.seller.id == authenticatedUser.id`. Unauthorized access throws `UnauthorizedException` (part of the sealed exception hierarchy).

### Build Process

Used parallel worktree agents after committing the foundation:

1. **Wave 1 (manual):** Migration V17, Listing entity update, ListingDto + frontend Listing type updated, TicketSeeder modified to assign sellers round-robin
2. **Waves 2-4 (2 parallel agents):** Backend agent (SellerController, service methods, DTOs, 27 tests) + Frontend agent (types, API, hooks, 3 pages, nav updates)
3. **Wave 5 (manual):** Playwright E2E tests (19 tests × 5 browsers = 91 assertions), accessibility fixes, llms.txt update, coverage gap tests

**Worktree merge lesson:** Agents branched from `main` (not the feature branch), so their `ListingService.java` lacked the Wave 1 `sellerDisplayName` mapping. Also caught a field name mismatch: frontend used `listedPrice` in request types but backend DTOs used `price`. Both fixed during merge.

**E2E debugging insights:**
- Playwright's `**` glob is greedy across path separators: `**/api/v1/listings` matched `/api/v1/my/listings`. Fixed by switching to regex with `$` anchoring.
- Mobile-first `md:hidden` / `hidden md:block` creates two DOM trees. `getByText().first()` finds the hidden mobile element on desktop. Fixed with `isMobile` branching and table-scoped selectors.
- Axe-core caught icon-only buttons (Pencil, X) missing `aria-label` in the listings table — real accessibility bug fixed.

### Deliverables

**Backend (30 files changed):**
- `V17__add_seller_to_listings.sql` — adds nullable `seller_id` FK with indexes
- `SellerController.java` — 5 endpoints with OpenAPI annotations
- 5 new DTOs: `SellListingRequest`, `SellerListingDto`, `UpdatePriceRequest`, `EarningsSummaryDto`, `SaleDto`
- `ListingService.java` — 5 seller methods + helpers (resolveUser, verifyOwnership, toSellerListingDto, toSaleDto)
- Repository additions: seller queries, seat-based ticket lookup, earnings aggregation
- `TicketSeeder.java` — assigns sellers to seeded listings round-robin
- `llms.txt` — seller endpoints added

**Frontend (16 files changed):**
- `SellPage.tsx` — 3-step form (event search → seat details → set price)
- `MyListingsPage.tsx` — tab-filtered table (desktop) / cards (mobile) with inline price editing
- `EarningsPage.tsx` — dashboard with summary stat cards and recent sales table
- `seller.ts` types, `seller.ts` API, `use-seller.ts` hooks
- Navigation updates (Header, MobileNav, router, constants)

**Tests:**
- 33 new backend tests (16 service + 11 controller + 6 coverage gap tests)
- `ListingService.java` at 100% line coverage (190/190)
- 19 Playwright E2E tests across 5 browsers (91 assertions, 4 desktop-only skips)
- Accessibility tests on all 3 seller pages (axe-core WCAG2A/2AA)

### Updated Metrics

| Category | Before | After |
|----------|--------|-------|
| Backend tests | 316 | 349 |
| Playwright E2E tests | 4 specs (91 assertions) | 5 specs (182 assertions) |
| Total test count | ~367 | ~400 |
| API endpoints | ~50 | ~55 |
| DTOs | 38 records | 43 records |
| Frontend pages | 17 | 20 |
| Flyway migrations | V0–V16 | V0–V17 |

### Commits (2026-03-20)

```
8d90a17 Merge feature/seller-flow: two-sided marketplace (#28)
6632c78 Add coverage tests for ListingService edge cases
22620e1 Add Playwright E2E tests for seller flow (19 tests, 5 browsers)
1fad927 Add seller endpoints to llms.txt for AI agent discovery
19c8b85 Add seller flow: endpoints, frontend pages, and tests
d9cd91f Add seller identity to listings for seller flow
```

---

## Session: 2026-03-20 (cont.) — Deployment, AI Tools, and Production Fixes

### Overview

Deployed MockHub to production on Railway after attempting Render (512MB RAM insufficient for Spring Boot 4). Fixed numerous deployment issues: JDBC URL format, port binding, CORS, SPA routing, security config, seed data, ephemeral filesystem images, and AI chat function-calling.

### Deployment Journey

**Render (attempted, abandoned):**
- 512MB RAM on starter tier ($7/mo) couldn't run Spring Boot 4 + Hibernate + Spring AI
- Even with `-Xmx200m -XX:+UseSerialGC`, the JVM was OOM-killed silently
- Next tier up was $25/mo (2GB) — too expensive for a teaching demo

**Railway (successful, $5/mo):**
- Hobby plan: up to 8GB RAM per replica, $5 minimum
- PostgreSQL add-on with internal networking
- Auto-deploy from GitHub pushes

### Production Issues Fixed (in order)

1. **JDBC URL format** — Railway/Render provide `postgresql://user:pass@host/db`, Spring JDBC needs `jdbc:postgresql://host:port/db` with separate username/password properties
2. **Port binding** — Render assigns dynamic `PORT` env var; added `server.port: ${PORT:8080}` to prod profile
3. **OOM on Render** — 512MB too small; switched to Railway
4. **JWT secret** — Railway auto-generated a secret with dots (`.`), which are invalid Base64 for JJWT
5. **Missing mock-payment profile** — `PaymentService` bean not found without `mock-payment` in active profiles
6. **Database URL format on Railway** — Split `DATABASE_URL` into `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` reference variables
7. **Security blocking static files** — `.anyRequest().authenticated()` blocked `index.html`, CSS, JS. Added `permitAll()` for static resources and SPA routes
8. **SPA handler intercepting API routes** — `/**` resource handler served `index.html` for `/api/v1/auth/login`, causing 403. Excluded `api/`, `actuator/`, `mcp/` paths
9. **Data seeder gated to dev profile** — Changed `@Profile("dev")` to `@Profile({"dev", "prod"})`
10. **CORS rejecting production origin** — CorsFilter only allowed `localhost:5173`. Added Railway domain to prod profile
11. **Ephemeral filesystem images** — Seed images lost on container redeploy. Added `restoreSeedImages()` that runs every startup
12. **AI chat had no database access** — Wired `EventTools` and `PricingTools` into ChatClient via `.defaultToolCallbacks()`
13. **SellerListingDto missing listedAt** — Frontend expected `listedAt` but DTO only had `createdAt`, causing "Invalid Date"

### Other Changes

- **Removed pgvector dependency** — Neutralized V0/V14 migrations, dropped vector_store table (V18/V19), switched Docker/Testcontainers/CI to `postgres:17`
- **Removed unused EmbeddingService** — Was scaffolded but never called
- **AiConfig refactor** — Wire `AnthropicChatModel` directly instead of `ChatClient.Builder` (no more `@Primary` workaround)
- **Chat starter prompts** — Clickable example queries in empty chat widget state
- **57 SonarCloud issues fixed** — Unused imports, throws, patterns, regex
- **CI concurrency groups** — New pushes auto-cancel previous in-progress runs
- **Documented embedding/provider decisions on issue #9** — Future: one config per chat provider, OpenAI `text-embedding-3-small` for embeddings regardless of chat provider

### New Issues Created

| # | Issue |
|---|-------|
| 33 | Sell form: show valid sections and surface seat-not-found errors |
| 34 | My Listings table: action columns hidden on narrow viewports |

---

## Session: 2026-03-21 — UX Polish, Tests, and Documentation Cleanup

### Overview

GPT-5.4 reviewed the live site and provided UX feedback. Implemented two small polish items, added missing test coverage, and significantly cleaned up all three documentation files.

### UX Changes (#35)

1. **Favorites tooltip** — Disabled heart buttons now show "Log in to save favorites" on hover when unauthenticated. Uses shadcn Tooltip (Radix) with a `<span>` wrapper to handle the disabled-element pointer-events gotcha.
2. **Hero CTA** — Added prominent "Browse Events" button below the search bar in the hero section, visible above the fold.

### Test Coverage Added

- `FavoriteButton.test.tsx` (10 tests) — rendering, disabled states, tooltip on hover, toggle behavior, accessible labels
- `HomePage.test.tsx` (6 tests) — hero headline, description, search input, Browse Events CTA, section headings
- Frontend tests: 38 → 54 (9 test files)

**Testing infrastructure:**
- Added `ResizeObserver` stub to `src/test/setup.ts` — required for Radix UI popper/tooltip components in jsdom
- Added `TooltipProvider` to `test-utils.tsx` `renderWithProviders` wrapper (mirrors `App.tsx`)
- Lesson: Radix tooltips render content in two DOM locations (visible + accessible hidden `role="tooltip"`). Use `findByRole('tooltip')` not `findByText` to avoid duplicate-match failures.

### Documentation Cleanup

| File | Before | After | Reduction |
|------|--------|-------|-----------|
| README.md | 319 lines | 128 lines | 60% |
| ARCHITECTURE.md | 2849 lines | 644 lines | 77% |
| CLAUDE.md | 165 lines | 164 lines | minor |

**README.md:** Removed DOP section, duplicate accounts tables, detailed env vars, detailed seller endpoints. Consolidated quick start. Added "Further Reading" links to ARCHITECTURE.md and CLAUDE.md. Kept all 8 screenshots.

**ARCHITECTURE.md:** Removed historical build artifacts: agent team organization (section 9), build phases (section 7), 375-line detailed file tree, 375-line table definitions (duplicating Flyway), 220-line Docker/YAML configs (duplicating actual files). Kept: ER diagrams, API tables, architecture patterns, pricing engine, key decisions.

**CLAUDE.md:** Removed stale `render.yaml` reference, updated ARCHITECTURE.md description, fixed stale "section 4.10" reference, removed hardcoded test count.

### Process Note

The UX changes were implemented directly on main without TDD or a prior GitHub issue — both violations of established workflow rules (see feedback memories). Issue #35 was created retroactively for documentation. Tests were written after implementation rather than before.

### Commits (2026-03-21)

```
0013567 Add UX polish (favorites tooltip, hero CTA), tests, and docs cleanup
```

---

---

## Session: 2026-03-21 (cont.) — Interactive Venue Seat Maps (#36)

### Overview

Added section-level interactive SVG venue maps to the EventDetailPage. Any ticket marketplace needs seat maps — GPT-4.5 recommended a phased approach, and this implements Phase 1: clickable section-level maps with availability and pricing overlays.

### Architecture

**Backend:**
- Added 5 SVG fields to `SectionAvailabilityDto`: `svgPathId`, `svgX`, `svgY`, `svgWidth`, `svgHeight`
- Extended JPQL query and mapping in `TicketRepository`/`TicketService`
- `VenueSeeder` computes stacked-bar layout coordinates (600x400 canvas) for all 76 sections across 22 venues
- `V20__populate_section_svg_coordinates.sql` — Flyway migration using window functions to populate SVG data for existing databases

**Frontend:**
- `VenueMap.tsx` — SVG component with accessible sections (`role="button"`, `tabIndex`, keyboard Enter/Space, `aria-label` per section)
- Graceful fallback to `SeatSelector` card grid when SVG data is missing
- EventDetailPage integration: "Venue Map" tab replaces "Sections" tab, click section → "View tickets" button → switches to Tickets tab with section filter
- `TicketListView` gains `sectionFilter`/`onClearFilter` props with a dismissable filter badge

**Layout algorithm (all venue types):**
- Canvas: 600x400, stage indicator at top (y=5, h=30)
- Sections stack vertically as colored horizontal bars
- Position computed from section count: `sectionHeight = (350 - (N-1) * 8) / N`

### Bug Found During Testing

The `SectionAvailability` TypeScript interface had wrong field names (`id`/`name`/`availableCount`) that didn't match the backend DTO (`sectionId`/`sectionName`/`availableTickets`). The old `SeatSelector` component happened to work because JavaScript silently returns `undefined` for missing properties. Fixed across all consuming components.

### Visual Fix

Initial implementation used white text on pastel section colors at 0.7 opacity — poor contrast. Fixed to dark text (`#1a1a1a`) on 0.85 opacity fills. Also replaced CSS custom property colors (`hsl(var(--muted))`) with hex values that work reliably in SVG `fill` attributes.

### Parallelization

Used two parallel subagents after establishing the DTO contract:
- Backend agent: seeder SVG population + Flyway migration
- Frontend agent: TypeScript types + VenueMap component + 6 tests

Both completed independently, then integration and fixes done sequentially.

### Deliverables

**Modified files (8):** SectionAvailabilityDto, TicketRepository, TicketService, TicketServiceTest, VenueSeeder, ticket.ts types, EventDetailPage, TicketListView, SeatSelector, TicketGridView

**New files (3):** V20 migration, VenueMap.tsx, VenueMap.test.tsx

**Tests:** 1 new backend test (SVG field mapping), 6 new VenueMap component tests — 60 frontend tests total

### Commits (2026-03-21)

```
940f0e9 Add interactive section-level venue maps (#36)
c163f67 Fix SectionAvailability field names to match API response
3a8fa86 Fix venue map contrast: dark text on colored sections, solid stage bar
```

### Future (Phase 2-3)

- Type-specific layouts (concentric arcs for arenas, fan shapes for theaters)
- Row-level interactivity for 1-2 showcase venues
- Zoom/pan for large venues
- Price heatmap overlay

---

## Session: 2026-03-22 — Ticket Delivery Completion, Email, Public Ticket View

### Summary

Completed all three phases of ticket delivery (PDF, SMS, email) and added a public ticket view page accessible from SMS/email links without authentication. Fixed two UX bugs in the checkout flow. Added GitHub link to footer.

### Changes

1. **GitHub repo link in footer** — lucide-react `Github` icon, `target="_blank"` with `noopener noreferrer`, updated tagline to "Open source & built for learning"

2. **Twilio SMS integration test** — `TwilioSmsDeliveryServiceIntegrationTest` sends a real SMS, gated by `@Tag("twilio")` and `@EnabledIfEnvironmentVariable`. Gradle config excludes `twilio` tag by default; run with `./gradlew test -PincludeTags=twilio`. Discovered Twilio account was suspended for billing/compliance — resolved via Twilio support ticket.

3. **`SmsDeliveryService.sendSms()` returns String** — returns provider message SID (or null on failure) instead of void. Mock implementation returns `MOCK-SID-{timestamp}`.

4. **Fixed mock checkout flow** — Frontend was only calling `/orders/checkout` without calling `createPaymentIntent` + `confirmPayment`, leaving orders permanently PENDING. Now follows the same three-step flow as Stripe: checkout → createPaymentIntent → confirmPayment.

5. **Fixed checkout page flash** — Empty cart state briefly appeared during mock payment processing because React Query refetched the (now-empty) cart between checkout and payment confirmation. Added `isMockProcessing` guard to the empty state condition.

6. **Public ticket view page** — New `/tickets/view?token={orderViewToken}` route (no auth). Order-view JWT tokens (`typ: "order-view"`) are distinct from per-ticket verification tokens. `PublicTicketViewController` serves order details and QR code PNGs. Mobile-optimized `PublicTicketViewPage` component. SMS and email now link here instead of the authenticated order confirmation page.

7. **Email delivery** — `EmailDeliveryService` interface with `MockEmailDeliveryService` (mock-email profile) and `SmtpEmailDeliveryService` (email-smtp profile). Uses Spring `JavaMailSender` with Resend SMTP (`smtp.resend.com:465`). HTML email includes order summary and "View Your Tickets" button. Added `spring-boot-starter-mail` dependency.

8. **Railway environment updates** — Added `RESEND_API_KEY`, `EMAIL_FROM_ADDRESS`, updated `SPRING_PROFILES_ACTIVE` to `prod,ai-anthropic,mock-payment,sms-twilio,email-smtp`. Updated `TWILIO_AUTH_TOKEN` after account reactivation.

### Challenges

- **Twilio account suspended** — billing/compliance issue from inactivity. Auth token returned 401 despite showing correctly in console. Resolved by Twilio support within 30 minutes of filing a ticket. The integration test caught the auth failure correctly.
- **Twilio API key creation** — Console UI kept erroring when creating Standard/Main API keys. Ended up using the existing auth token (2-param `Twilio.init`) instead of the 3-param API key approach.
- **Mock checkout PENDING bug** — The mock payment flow never called `confirmPayment`, discovered only during live testing on Railway. Unit tests all passed because they tested each step independently — classic integration gap.

### Commits (2026-03-22)

```
120f395 Add GitHub repository link to footer
de49f4c Add Twilio SMS integration test and return SID from sendSms
6b6478c Fix mock checkout to confirm payment before showing order
82bd290 Add public ticket view page accessible from SMS link
bc4e9ab Fix checkout page flash during mock payment processing
5230f05 Add email delivery on order confirmation via Spring Mail
```

### GitHub Issues Created

- #46 — Audit mobile responsiveness of current web app
- #47 — Evaluate mobile app strategy: native, React Native, or KMP

---

*Last updated: 2026-03-22*
*Built with: Claude Opus 4.6 (1M context) via Claude Code*
*~450 tests passing (443 backend + 64 frontend unit + Playwright E2E)*
*Live at: https://mockhub.kousenit.com*
