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

## Final Codebase Metrics

### File Counts

| Category | Count |
|----------|-------|
| Backend Java (production) | 139 |
| Backend Java (test) | 22 |
| Frontend TypeScript/TSX (production) | 105 |
| Frontend component tests | 5 |
| Playwright E2E specs | 4 |
| Flyway migrations | 15 (V0–V14) |
| React components | 50 |
| **Total files** | **~340** |

### Line Counts

| Category | Lines |
|----------|-------|
| Backend Java (all) | ~12,100 |
| Frontend TypeScript/TSX (all) | ~8,700 |
| **Total code** | **~20,800** |

### Backend Architecture

| Layer | Count | Examples |
|-------|-------|---------|
| Controllers | 15 | Auth, Event, Venue, Cart, Order, Payment, Favorite, Notification, Admin, Search, Price, Category, Tag, AI, Image |
| Services | 19 | AuthService, EventService, CartService, OrderService, PricingEngine, NotificationService, AdminService, etc. |
| Entities | 23 | User, Role, Venue, Section, SeatRow, Seat, Event, Category, Tag, Ticket, Listing, Cart, CartItem, Order, OrderItem, Favorite, Notification, Image, TransactionLog, PriceHistory |
| DTOs (records) | 38 | Request/Response records with Jakarta validation |
| Repositories | 16 | Spring Data JPA interfaces |
| Feature packages | 16 | admin, ai, auth, cart, common, config, event, favorite, image, notification, order, payment, pricing, search, seed, ticket, venue |

### Frontend Architecture

| Layer | Count |
|-------|-------|
| API modules | 11 (auth, cart, client, events, favorites, notifications, orders, payments, search, venues, admin) |
| React Query hooks | 9 (use-auth, use-cart, use-events, use-favorites, use-notifications, use-orders, use-pagination, use-search, use-admin) |
| Type definitions | 10 (auth, cart, common, event, favorite, notification, order, ticket, venue, admin) |
| Pages | 14 (Home, Login, Register, EventList, EventDetail, Cart, Checkout, OrderConfirmation, OrderHistory, Favorites, NotFound, AdminDashboard, AdminEvents, AdminEventForm, AdminUsers) |
| Zustand stores | 3 (auth-store, cart-store, ui-store) |

### Test Coverage

| Test Type | Files | Description |
|-----------|-------|-------------|
| Backend unit tests | 11 | JUnit 5 + Mockito for all services |
| Backend controller tests | 6 | MockMvc + @WebMvcTest |
| Backend integration tests | 5 | Testcontainers PostgreSQL (auth, events, cart-checkout, admin flows) |
| Frontend component tests | 5 | Vitest + React Testing Library |
| Playwright E2E | 4 | Auth, browsing, cart-checkout, accessibility (axe-core) |
| **E2E browsers** | **5** | Chrome, Firefox, Safari (WebKit), Mobile Android (Pixel 5), Mobile iOS (iPhone 12) |

### Seed Data

| Entity | Count |
|--------|-------|
| Users | 8 (admin, buyer, seller, 5 random) |
| Venues | 22 (arenas, theaters, amphitheaters, clubs, stadiums) |
| Events | 100+ (concerts, sports, theater, comedy, festivals) |
| Tickets/Listings | Generated per event (~35% of seats listed) |

### API Endpoints

| Group | Endpoints |
|-------|-----------|
| Auth | 4 (register, login, refresh, me) |
| Events | 6 (list, detail, search, create, update, featured) |
| Venues | 3 (list, detail, sections) |
| Cart | 4 (get, add, remove, clear) |
| Orders | 3 (checkout, list, detail) |
| Payments | 3 (create intent, confirm, webhook) |
| Favorites | 4 (list, add, remove, check) |
| Notifications | 4 (list, unread count, mark read, mark all read) |
| Admin | 10 (dashboard, event CRUD, user management, orders, ticket generation) |
| Search | 2 (search, suggest) |
| Pricing | 1 (price history) |
| Images | 2 (serve, thumbnail) |
| AI (stubs) | 4 (recommendations, NL search, chat, price prediction) |
| **Total** | **~50** |

---

## Commit History

```
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

The Flyway migration V0 creates the `vector` extension and V14 uses the `vector` column type. Integration tests using plain `postgresql:17` fail on these migrations. Resolution: switched from Testcontainers JDBC URL approach to programmatic `@Container` with `pgvector/pgvector:pg17` Docker image and `@DynamicPropertySource`.

### AI Auto-Configuration in Tests

Integration tests don't need AI features. The Spring AI auto-configurations must be explicitly excluded using `spring.autoconfigure.exclude` in `@SpringBootTest(properties = {...})` since property-based disabling (`spring.ai.openai.enabled=false`) is not supported by Spring AI 2.0.0-M3. The actual auto-configuration class names differ from what you might guess:
- `org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration`
- `org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration`
- `org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration`

### Test Results After Fixes

| Category | Total | Passing | Failing |
|----------|-------|---------|---------|
| Backend unit tests (Mockito) | 88 | 88 | 0 |
| Backend controller tests (MockMvc) | 20 | 20 | 0 |
| Backend integration tests (Testcontainers) | 14 | 4 | 10 |
| Frontend component tests (Vitest) | 26 | 26 | 0 |
| **Total** | **148** | **138** | **10** |

The 10 remaining integration test failures are related to Spring context initialization order and shared test context caching between integration test classes. These require further investigation of the security filter chain behavior under Spring Boot 4's new servlet module structure.

### Key Takeaway

**Never assume generated tests will pass.** The testing agent wrote syntactically valid tests with correct logic, but it used Spring Boot 3 conventions for imports, dependencies, and annotations. Spring Boot 4's modularization is the most significant breaking change for test code — every test dependency was restructured. The Spring Initializr is the authoritative source for correct dependency coordinates.

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

*Generated: 2026-03-17*
*Built with: Claude Opus 4.6 (1M context) via Claude Code*
