# Stripe Price IDs Configuration

## Step 1: Create Products in Stripe Dashboard

Go to: https://dashboard.stripe.com/test/products

### Product 1: Innovexia Plus
- **Monthly Price**: $9.99/month
  - Copy price ID: `price_________________` ← Paste here
- **Yearly Price**: $99.99/year (Save 15%)
  - Copy price ID: `price_________________` ← Paste here

### Product 2: Innovexia Pro
- **Monthly Price**: $19.99/month
  - Copy price ID: `price_________________` ← Paste here
- **Yearly Price**: $199.99/year (Save 15%)
  - Copy price ID: `price_________________` ← Paste here

### Product 3: Innovexia Master
- **Monthly Price**: $39.99/month
  - Copy price ID: `price_________________` ← Paste here
- **Yearly Price**: $399.99/year (Save 15%)
  - Copy price ID: `price_________________` ← Paste here

## Step 2: Update index.js

After creating products and copying price IDs above, update `index.js` line 17:

```javascript
function planToPriceId(planId, period) {
  const map = {
    'PLUS:MONTHLY': 'price_________________',    // ← Paste Plus monthly here
    'PLUS:YEARLY': 'price_________________',     // ← Paste Plus yearly here
    'PRO:MONTHLY': 'price_________________',     // ← Paste Pro monthly here
    'PRO:YEARLY': 'price_________________',      // ← Paste Pro yearly here
    'MASTER:MONTHLY': 'price_________________',  // ← Paste Master monthly here
    'MASTER:YEARLY': 'price_________________',   // ← Paste Master yearly here
  };
  return map[\`\${planId}:\${period}\`];
}
```

## Step 3: Restart Server

```bash
npm start
```

Done! ✅
