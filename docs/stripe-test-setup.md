# Stripe Test Mode Setup

## 1. Create a Stripe Account

Sign up at [dashboard.stripe.com](https://dashboard.stripe.com). No business verification is needed for test mode.

## 2. Enable Test Mode

Toggle **Test mode** in the top-right of the Stripe dashboard. New accounts default to test mode.

## 3. Get API Keys

Navigate to **Developers → API keys**:

- **Publishable key** (`pk_test_...`): used in the frontend (`VITE_STRIPE_PUBLISHABLE_KEY`)
- **Secret key** (`sk_test_...`): used in the backend (`STRIPE_SECRET_KEY`)

## 4. Set Up Webhooks (Optional)

### Option A: Stripe CLI (recommended for local development)

```bash
brew install stripe/stripe-cli/stripe
stripe login
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

The `stripe listen` command prints a webhook signing secret (`whsec_...`). Use this as `STRIPE_WEBHOOK_SECRET`.

### Option B: Dashboard

Go to **Developers → Webhooks → Add endpoint**, point it at your URL, and copy the signing secret.

## 5. Configure MockHub

Set environment variables (or add to `backend/.env`):

```
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

For the frontend (or add to `frontend/.env`):

```
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

Then run with the `stripe` profile to use real Stripe instead of mock payments:

```
SPRING_PROFILES_ACTIVE=dev,stripe
```

## 6. Test Card Numbers

Stripe test mode uses fake card numbers. No real money is charged.

| Card Number          | Scenario            |
|----------------------|---------------------|
| `4242 4242 4242 4242` | Successful payment  |
| `4000 0000 0000 3220` | 3D Secure required  |
| `4000 0000 0000 9995` | Declined             |

Use any future expiration date and any 3-digit CVC.

## References

- [Stripe Test Mode Docs](https://docs.stripe.com/test-mode)
- [Stripe CLI Docs](https://docs.stripe.com/stripe-cli)
- [Test Card Numbers](https://docs.stripe.com/testing#cards)
