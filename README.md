# MockHub

A secondary concert ticket marketplace built as a teaching platform for AI integration.

MockHub mimics the functionality of sites like StubHub and TicketNetwork — registration, event browsing, seat selection, dynamic pricing, and checkout — providing a realistic, full-featured codebase for students to build AI features on top of.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4, Java 25, Spring AI 2.0.0-M3 |
| Database | PostgreSQL 17 + pgvector |
| Frontend | React 19, TypeScript, Tailwind CSS, shadcn/ui |
| Build | Gradle 9.4.0, Vite |
| Testing | JUnit 5, Testcontainers, Vitest, Playwright |
| Payments | Stripe (test mode) |

## Prerequisites

- Java 25 ([Eclipse Temurin](https://adoptium.net/) recommended)
- Node.js 22+
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/kousen/mockhub.git
cd mockhub
```

### 2. Start the database

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts PostgreSQL with the pgvector extension on port 5432.

### 3. Run the backend

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=docker,mock-payment'
```

The API will be available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

### Running with H2 (no Docker needed)

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

This uses an in-memory H2 database. H2 console available at `http://localhost:8080/h2-console`.

## Full Docker Stack

To run everything in containers:

```bash
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Project Structure

```
mockhub/
├── backend/          # Spring Boot application
│   ├── src/main/java/com/mockhub/
│   │   ├── auth/     # Authentication (JWT, Spring Security)
│   │   ├── event/    # Events, categories, tags
│   │   ├── venue/    # Venues, sections, seats
│   │   ├── ticket/   # Tickets and listings
│   │   ├── pricing/  # Dynamic pricing engine
│   │   ├── cart/     # Shopping cart
│   │   ├── order/    # Orders and checkout
│   │   ├── payment/  # Stripe + mock payment
│   │   ├── favorite/ # User favorites
│   │   ├── notification/ # In-app notifications
│   │   ├── ai/       # AI stub endpoints (for student implementation)
│   │   ├── admin/    # Admin dashboard
│   │   ├── search/   # Full-text search
│   │   ├── image/    # Image storage
│   │   ├── seed/     # Seed data generation
│   │   ├── config/   # App configuration
│   │   └── common/   # Shared utilities, exceptions, base entity
│   └── src/test/
├── frontend/         # React application
│   ├── src/
│   │   ├── api/      # API client functions
│   │   ├── hooks/    # React Query hooks
│   │   ├── stores/   # Zustand state stores
│   │   ├── pages/    # Route pages
│   │   ├── components/ # UI components
│   │   ├── types/    # TypeScript type definitions
│   │   └── lib/      # Utilities
│   └── e2e/          # Playwright E2E tests
├── docker-compose.yml     # Full stack
├── docker-compose.dev.yml # Database only
└── ARCHITECTURE.md        # Detailed architecture plan
```

## Testing

### Backend

```bash
cd backend

# Unit + controller tests
./gradlew test

# Integration tests (requires Docker for Testcontainers)
./gradlew integrationTest
```

### Frontend

```bash
cd frontend

# Component tests
npm test

# E2E tests (requires full stack running)
npx playwright test

# E2E with specific browser
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
npx playwright test --project="Mobile Android"
npx playwright test --project="Mobile iOS"
```

## AI Integration Exercises

MockHub includes stub API endpoints designed for students to implement AI features:

| Endpoint | Exercise |
|----------|----------|
| `POST /api/v1/chat` | Customer support chatbot (Spring AI ChatClient) |
| `GET /api/v1/recommendations` | Event recommendations (vector similarity + collaborative filtering) |
| `POST /api/v1/search/natural-language` | Natural language search ("jazz concerts under $50 this weekend") |
| `GET /api/v1/events/{slug}/predicted-price` | ML-powered price prediction |

Additional AI exercise areas:
- Dynamic pricing model improvements
- Fraud detection on transactions
- Sentiment analysis on event reviews
- Image recognition for ticket verification

Spring AI is pre-configured with profiles for Anthropic Claude, OpenAI, and Ollama (local models).

## Environment Variables

Copy `.env.example` files in `backend/` and `frontend/` to `.env` and configure:

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | Yes | JWT signing key (min 256 bits) |
| `STRIPE_SECRET_KEY` | For Stripe | Stripe test secret key (`sk_test_...`) |
| `STRIPE_WEBHOOK_SECRET` | For Stripe | Stripe webhook signing secret |
| `ANTHROPIC_API_KEY` | For AI | Anthropic API key |
| `OPENAI_API_KEY` | For AI | OpenAI API key |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Code of Conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE)
