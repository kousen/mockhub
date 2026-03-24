# Friend Demo Checklist

Use this for a quick live walkthrough of the agentic commerce flow.

## Preflight

- Confirm the backend and frontend are running, or use the Railway deployment.
- Confirm seeded demo user works: `buyer@mockhub.com` / `buyer123`.
- Confirm the backend test suite is green:
  ```bash
  cd backend && ./gradlew test
  ```
- Confirm at least one active mandate can be created for the demo buyer.
- Confirm the target event exists in current seed data, for example a Yo-Yo Ma event in New York within the seeded future window.

## API-First Flow

1. Create a mandate for the acting agent.
2. Search offers through `GET /acp/v1/listings` with a query, city, and date window.
3. Choose the first returned listing.
4. Create checkout through `POST /acp/v1/checkout` with `agentId` and `mandateId`.
5. Complete checkout through `POST /acp/v1/checkout/{id}/complete`.
6. Show the confirmed order and ticket artifacts.

## Prompt-First Flow

1. Log in as the demo buyer.
2. Open the website chat assistant.
3. Ask for a time-bounded search, for example:
   `Find Yo-Yo Ma tickets in New York in the next month and buy the first option under $200 if available.`
4. Confirm the assistant used the purchase flow with mandate-backed tools.
5. Show the resulting order page, ticket download, and public ticket view.

## What To Verify Live

- The offer search returns listing-level results, not only event summaries.
- Autonomous purchase actions include both `agentId` and `mandateId`.
- The order reaches `CONFIRMED` only after payment completion.
- Ticket inventory is decremented once.
- The mandate spend total advances once.
- A duplicate completion attempt does not double-charge, double-sell, or double-notify.

## Fast Failure Checks

- If checkout creation fails, inspect mandate scope, budget, category/event restrictions, and listing availability.
- If completion fails on a non-mock payment method, confirm a payment intent was supplied.
- If the prompt flow stalls, verify the AI profile is enabled and the buyer is authenticated.
