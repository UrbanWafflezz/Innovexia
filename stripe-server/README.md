# Innovexia Stripe Test Server

Backend server for handling Stripe subscriptions in test mode.

## Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Configure Stripe Price IDs

1. Go to [Stripe Dashboard → Products](https://dashboard.stripe.com/test/products)
2. Create 3 products with recurring prices (or use existing ones):
   - **Plus**: Monthly and Yearly prices
   - **Pro**: Monthly and Yearly prices
   - **Master**: Monthly and Yearly prices

3. Note the price IDs (format: `price_xxxxxxxxxxxx`)

4. Update `index.js` in the `planToPriceId()` function with your actual price IDs:
```javascript
const map = {
  'PLUS:MONTHLY': 'price_xxxxxxxxxxxx',     // Your Plus monthly price ID
  'PLUS:YEARLY': 'price_xxxxxxxxxxxx',      // Your Plus yearly price ID
  'PRO:MONTHLY': 'price_xxxxxxxxxxxx',      // Your Pro monthly price ID
  'PRO:YEARLY': 'price_xxxxxxxxxxxx',       // Your Pro yearly price ID
  'MASTER:MONTHLY': 'price_xxxxxxxxxxxx',   // Your Master monthly price ID
  'MASTER:YEARLY': 'price_xxxxxxxxxxxx',    // Your Master yearly price ID
};
```

### 3. Configure Environment Variables

The `.env` file is already configured with your test keys. For webhooks:

1. Install Stripe CLI: https://stripe.com/docs/stripe-cli
2. Run: `stripe listen --forward-to http://localhost:4242/billing/webhook`
3. Copy the webhook signing secret (starts with `whsec_`)
4. Update `.env`:
```
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here
```

## Running the Server

```bash
npm start
```

Server will run on `http://localhost:4242`

For Android emulator, use: `http://10.0.2.2:4242`

## API Endpoints

### POST /billing/bootstrap
Initialize customer and get PaymentSheet configuration
```json
{
  "uid": "user-123",
  "email": "user@example.com"
}
```

### POST /billing/subscribe
Create subscription after PaymentSheet success
```json
{
  "uid": "user-123",
  "planId": "PLUS",
  "period": "MONTHLY"
}
```

### POST /billing/cancel
Cancel subscription at period end
```json
{
  "uid": "user-123"
}
```

### POST /billing/resume
Resume a canceled subscription
```json
{
  "uid": "user-123"
}
```

### POST /billing/restore
Restore purchases from Stripe
```json
{
  "uid": "user-123"
}
```

## Testing with Stripe CLI

Forward webhooks to local server:
```bash
stripe listen --forward-to http://localhost:4242/billing/webhook
```

Trigger test events:
```bash
stripe trigger payment_intent.succeeded
stripe trigger customer.subscription.created
```

## Security Notes

⚠️ **IMPORTANT**:
- Keep `sk_test_...` secret key ONLY on the server
- Never commit `.env` file to version control
- Use webhook secrets in production
- This is TEST MODE only - rotate keys before production

## Production Checklist

- [ ] Replace test keys with live keys
- [ ] Set up production webhook endpoint
- [ ] Implement proper database persistence
- [ ] Add authentication/authorization
- [ ] Implement rate limiting
- [ ] Add request validation
- [ ] Set up monitoring and logging
- [ ] Review Google Play Billing policies
