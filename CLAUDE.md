# MockHub — Project Instructions for Claude Code

## Project Overview

MockHub is a secondary concert ticket marketplace (like StubHub) built as a teaching platform for undergraduate AI students. The codebase must be clean, well-organized, and approachable for students reading it for the first time.

## Tech Stack

- **Backend:** Spring Boot 4, Java 25, Gradle 9.4.0 (Kotlin DSL)
- **Database:** PostgreSQL 17 with pgvector extension (Docker: `pgvector/pgvector:pg17`), H2 for dev profile
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
- **Error handling** goes through `GlobalExceptionHandler` (`@RestControllerAdvice`). Custom domain exceptions map to appropriate HTTP status codes.
- **Caching:** Use Spring `@Cacheable` annotations. Never cache carts, orders, notifications, or pricing data.
- **Spring profiles** control implementation variants:
  - `dev` — H2, mock payment, debug logging
  - `docker` — PostgreSQL, Stripe payment
  - `test` — Testcontainers PostgreSQL, mock payment
  - `mock-payment` / `stripe` — payment implementation
  - `ai-anthropic` / `ai-openai` / `ai-ollama` — AI provider

### Frontend

- **TypeScript** — no `any` types. Use proper interfaces defined in `src/types/`.
- **React Query (TanStack)** for all server state. Zustand only for client-side UI state (auth token, cart drawer, mobile nav).
- **API layer:** `src/api/*.ts` files export typed functions. `src/hooks/use*.ts` files wrap them in React Query hooks. Components use hooks, never call API functions directly.
- **shadcn/ui components** live in `src/components/ui/`. Custom components live in feature folders (`events/`, `cart/`, `checkout/`, etc.).
- **Mobile-first** responsive design using Tailwind breakpoints.
- **No inline styles.** Use Tailwind utility classes exclusively.

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

## CI / Quality

- **GitHub Actions** (`.github/workflows/ci.yml`) runs on push to main and PRs: backend tests, frontend lint/typecheck/tests, SonarCloud analysis, Docker build smoke test
- **SonarCloud** — org: `kousen-it-inc`, project: `kousen_mockhub`. Config in `sonar-project.properties`. Token stored as `SONAR_TOKEN` GitHub secret.
- **SonarQube MCP server** is available in this project. Use it to query SonarCloud for issues, quality gate status, and hotspots. When fixing code, check SonarCloud issues first.
- **GitHub repo:** `kousen/mockhub` (public, MIT license)

## Build Organization

The project is built in 5 waves of parallel agents (see ARCHITECTURE.md section 9):
1. **Wave 1: Foundation** — Spring Boot + React scaffolding, auth, database, Docker
2. **Wave 2: Catalog** — Events, venues, search, pricing engine
3. **Wave 3: Commerce** — Cart, checkout, payments
4. **Wave 4: Features** — Favorites, notifications, admin dashboard
5. **Wave 5: Polish** — Seed data, images, responsive polish, full test suite

Each wave's agents run with `mode: "bypassPermissions"` in isolated worktrees, then merge back before the next wave starts.

## File Reference

- `ARCHITECTURE.md` — Complete architecture plan with database schema, API design, component hierarchy, build phases, and agent team organization
- `sonar-project.properties` — SonarCloud configuration
- `.github/workflows/ci.yml` — CI pipeline
- `docker-compose.yml` — Full stack (Postgres, backend, frontend)
- `docker-compose.dev.yml` — Postgres only (for local development)

## What NOT to Do

- Do not use `var` in Java (use explicit types for readability — this is a teaching codebase)
- Do not add dependencies not listed in ARCHITECTURE.md section 4.10 without justification
- Do not use `localStorage` for JWT tokens (security anti-pattern — use in-memory + HttpOnly refresh cookie)
- Do not write Flyway migrations with `ddl-auto: create` or `update` (always `validate`)
- Do not create REST endpoints that bypass the service layer
- Do not suppress exceptions silently — log or rethrow as domain exceptions
- Do not use `@Autowired` on fields — use constructor injection (implicit with single constructor)
