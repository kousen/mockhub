# MockHub — Project Instructions for Claude Code

## Project Overview

MockHub is a secondary concert ticket marketplace (like StubHub) built as a teaching platform for undergraduate AI students. The codebase must be clean, well-organized, and approachable for students reading it for the first time.

## Tech Stack

- **Backend:** Spring Boot 4, Java 25, Gradle 9.4.0 (Kotlin DSL)
- **Database:** PostgreSQL 17 with pgvector extension (Docker: `pgvector/pgvector:pg17`)
- **AI:** Spring AI 2.0.0-M3 (milestone — requires Spring Milestones repo in Gradle)
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack React Query, Zustand
- **Testing:** JUnit 5 + Mockito + Testcontainers (backend), Vitest + React Testing Library + MSW (frontend), Playwright (E2E, 5 browsers)
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
  - `dev` — PostgreSQL, mock payment, debug logging, AI disabled (default). Profile group: `dev,mock-payment`
  - `test` — Testcontainers PostgreSQL, mock payment
  - `mock-payment` / `stripe` — payment implementation (`@Primary` on Stripe resolves conflicts when both active)
  - `ai-anthropic` / `ai-openai` / `ai-ollama` — AI provider (each overrides the base AI exclusion list to enable only its own provider)
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
- **AI responses are parsed from JSON.** Services prompt the LLM for JSON output and parse with Jackson. Fallback logic returns safe defaults if parsing fails.
- **Circular dependency:** MCP tool registration → PricingTools → PricePredictionService → ChatClient creates a cycle. Broken with `@Lazy` on the `ChatClient` parameter in `PricePredictionService`.

### Seller Flow

- **Any authenticated user can sell.** No separate seller role — `seller_id` is nullable on `listings` (NULL = platform/primary-market listing, non-null = user-created resale listing).
- **Seller creates listing by describing seat:** `POST /api/v1/listings` with eventSlug, sectionName, rowLabel, seatNumber, price. Backend finds the matching Ticket, validates availability, sets seller, creates Listing.
- **Ownership enforcement:** Update price and deactivate check `listing.seller.id == authenticated user`. Throws `UnauthorizedException` on mismatch.
- **Duplicate prevention:** `existsByTicketIdAndStatus(ticketId, "ACTIVE")` rejects re-listing an already-listed ticket.
- **Earnings aggregation:** Computed from `OrderItem` where `listing.seller.id` matches and `order.status = 'COMPLETED'`.
- **Endpoints:** `POST /api/v1/listings`, `GET /api/v1/my/listings`, `PUT /api/v1/listings/{id}/price`, `DELETE /api/v1/listings/{id}`, `GET /api/v1/my/earnings`
- **Frontend:** 3 pages (SellPage with 3-step form, MyListingsPage with tab filtering, EarningsPage dashboard), seller API + hooks, nav links visible when authenticated.

### Agentic Support

- **`llms.txt`** — served at `/llms.txt` (static resource), describes all API endpoints for AI agents (including seller endpoints)
- **RFC 9457 Problem Details** — all error responses use Spring's `ProblemDetail` format for machine-readable errors
- **MCP server** — 13 tools registered (EventTools, PricingTools, CartTools, OrderTools) via `spring-ai-starter-mcp-server-webmvc`. API key auth on `/mcp/**` via `McpApiKeyFilter`. **SSE transport returns 500 — needs investigation** (may need different transport for Spring AI M3).
- **MCP tools identify users by email** — cart and order tools accept `userEmail` parameter, not auth tokens

### Frontend

- **TypeScript** — no `any` types. Use proper interfaces defined in `src/types/`.
- **React Query (TanStack)** for all server state. Zustand only for client-side UI state (auth token, cart drawer, mobile nav).
- **API layer:** `src/api/*.ts` files export typed functions. `src/hooks/use*.ts` files wrap them in React Query hooks. Components use hooks, never call API functions directly.
- **shadcn/ui components** live in `src/components/ui/`. Custom components live in feature folders (`events/`, `cart/`, `checkout/`, `sell/`, etc.).
- **Mobile-first** responsive design using Tailwind breakpoints.
- **No inline styles.** Use Tailwind utility classes exclusively.
- **Error responses:** Frontend `ApiError` type uses `detail` field (RFC 9457), not `message`.

### Testing

- **Every feature must have tests.** This is non-negotiable.
- Backend unit tests: JUnit 5 + Mockito, naming convention `methodName_givenCondition_expectedResult`
- Backend controller tests: MockMvc + @WebMvcTest
- Backend integration tests: Testcontainers with real PostgreSQL
- Frontend component tests: Vitest + React Testing Library, colocated as `*.test.tsx`
- Frontend API mocking: MSW (Mock Service Worker)
- E2E tests: Playwright configured for Chrome, Firefox, Safari, Mobile Android, Mobile iOS
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
- **SonarCloud** — org: `kousen-it-inc`, project: `kousen_mockhub`. Config in both `sonar-project.properties` (frontend) and `build.gradle.kts` sonar block (backend). Token stored as `SONAR_TOKEN` GitHub secret.
- **Issue exclusions** are in the Gradle `sonar {}` block: S1186 (JPA empty constructors), S1192 (seed data literals), S3776 (seed data complexity). Do NOT add them only to `sonar-project.properties` — the backend scanner won't see them there.
- **SonarQube MCP server** is available in this project. Use it to query SonarCloud for issues, quality gate status, and hotspots. When fixing code, check SonarCloud issues first.
- **GitHub repo:** `kousen/mockhub` (public, MIT license)

## File Reference

- `ARCHITECTURE.md` — Complete architecture plan with database schema, API design, component hierarchy, build phases, and agent team organization
- `PROJECT_JOURNAL.md` — Build report with session notes, challenges, metrics, and commit history
- `docs/stripe-test-setup.md` — Stripe test mode API key setup instructions
- `sonar-project.properties` — SonarCloud configuration for frontend (coverage exclusions, issue suppressions)
- `backend/build.gradle.kts` — Backend build config including SonarCloud issue exclusions in `sonar {}` block
- `.github/workflows/ci.yml` — CI pipeline (backend tests incl. Testcontainers, frontend lint/typecheck/tests, SonarCloud, Docker build)
- `docker-compose.yml` — Full stack (Postgres, backend, frontend)
- `docker-compose.dev.yml` — Postgres only (for local development)
- `backend/src/main/resources/static/llms.txt` — API description for AI agent discovery

## What NOT to Do

- Do not use `var` in Java (use explicit types for readability — this is a teaching codebase)
- Do not add dependencies not listed in ARCHITECTURE.md section 4.10 without justification
- Do not use `localStorage` for JWT tokens (security anti-pattern — use in-memory + HttpOnly refresh cookie)
- Do not write Flyway migrations with `ddl-auto: create` or `update` (always `validate`)
- Do not create REST endpoints that bypass the service layer
- Do not suppress exceptions silently — log or rethrow as domain exceptions
- Do not use `@Autowired` on fields — use constructor injection (implicit with single constructor)
- Do not seal interfaces that need Mockito mocking in `@WebMvcTest` tests (Mockito can't create subclasses of sealed types)
- Do not add SonarCloud issue exclusions only to `sonar-project.properties` — backend issues need the Gradle `sonar {}` block
