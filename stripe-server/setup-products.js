import 'dotenv/config';
import Stripe from 'stripe';
import { writeFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Parse command line arguments
const args = process.argv.slice(2);
const mode = args.includes('--production') || args.includes('--prod') ? 'production' : 'test';

// Load appropriate environment
if (mode === 'production') {
  console.log('🔴 PRODUCTION MODE - Using live Stripe keys');
  // Load production environment if exists
  const prodEnvPath = join(__dirname, '.env.production');
  if (existsSync(prodEnvPath)) {
    const dotenv = await import('dotenv');
    dotenv.config({ path: prodEnvPath });
  }
} else {
  console.log('🟢 TEST MODE - Using test Stripe keys');
}

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, { apiVersion: '2024-06-20' });

// Validate API key matches mode
const isTestKey = process.env.STRIPE_SECRET_KEY?.startsWith('sk_test_');
const isLiveKey = process.env.STRIPE_SECRET_KEY?.startsWith('sk_live_');

if (mode === 'production' && !isLiveKey) {
  console.error('❌ ERROR: Production mode requires a live secret key (sk_live_...)');
  console.error('   Current key starts with:', process.env.STRIPE_SECRET_KEY?.substring(0, 8));
  process.exit(1);
}

if (mode === 'test' && !isTestKey) {
  console.error('❌ ERROR: Test mode requires a test secret key (sk_test_...)');
  console.error('   Current key starts with:', process.env.STRIPE_SECRET_KEY?.substring(0, 8));
  process.exit(1);
}

async function setupProducts() {
  console.log(`\n🚀 Creating Stripe products and prices for ${mode.toUpperCase()} environment...\n`);

  const products = [
    {
      name: 'Innovexia Plus',
      description: 'Unlock more intelligence',
      prices: [
        { amount: 999, interval: 'month', key: 'PLUS:MONTHLY' },
        { amount: 9999, interval: 'year', key: 'PLUS:YEARLY' }
      ]
    },
    {
      name: 'Innovexia Pro',
      description: 'Your AI, fully realized',
      prices: [
        { amount: 1999, interval: 'month', key: 'PRO:MONTHLY' },
        { amount: 19999, interval: 'year', key: 'PRO:YEARLY' }
      ]
    },
    {
      name: 'Innovexia Master',
      description: 'Unleash everything',
      prices: [
        { amount: 3999, interval: 'month', key: 'MASTER:MONTHLY' },
        { amount: 39999, interval: 'year', key: 'MASTER:YEARLY' }
      ]
    }
  ];

  const priceIdMap = {};

  for (const productData of products) {
    try {
      // Check if product already exists
      const existingProducts = await stripe.products.search({
        query: `name:'${productData.name}' AND active:'true'`,
      });

      let product;
      if (existingProducts.data.length > 0) {
        product = existingProducts.data[0];
        console.log(`ℹ️  Product already exists: ${product.name} (${product.id})`);
      } else {
        // Create product
        product = await stripe.products.create({
          name: productData.name,
          description: productData.description,
        });
        console.log(`✅ Created product: ${product.name} (${product.id})`);
      }

      // Create or find prices for this product
      for (const priceData of productData.prices) {
        // Check if price already exists for this product
        const existingPrices = await stripe.prices.list({
          product: product.id,
          active: true,
        });

        const matchingPrice = existingPrices.data.find(p =>
          p.unit_amount === priceData.amount &&
          p.recurring?.interval === priceData.interval
        );

        let price;
        if (matchingPrice) {
          price = matchingPrice;
          const displayAmount = (priceData.amount / 100).toFixed(2);
          console.log(`  ℹ️  Price already exists: $${displayAmount}/${priceData.interval} (${price.id})`);
        } else {
          price = await stripe.prices.create({
            product: product.id,
            unit_amount: priceData.amount,
            currency: 'usd',
            recurring: {
              interval: priceData.interval
            }
          });

          const displayAmount = (priceData.amount / 100).toFixed(2);
          console.log(`  ✅ Created ${priceData.interval}ly price: $${displayAmount} (${price.id})`);
        }

        priceIdMap[priceData.key] = price.id;
      }

      console.log('');
    } catch (error) {
      console.error(`❌ Error creating ${productData.name}:`, error.message);
    }
  }

  // Save price mapping to JSON file
  const configFileName = mode === 'production' ? 'price-ids.production.json' : 'price-ids.test.json';
  const configPath = join(__dirname, configFileName);

  const config = {
    mode,
    updated: new Date().toISOString(),
    priceIds: priceIdMap
  };

  writeFileSync(configPath, JSON.stringify(config, null, 2));
  console.log(`✅ Saved price IDs to ${configFileName}\n`);

  // Display mapping
  console.log('📋 Price ID Mapping:');
  console.log('Copy these into your index.js planToPriceId function:\n');
  console.log('function planToPriceId(planId, period) {');
  console.log('  const map = {');
  console.log(`    'PLUS:MONTHLY': '${priceIdMap['PLUS:MONTHLY']}',`);
  console.log(`    'PLUS:YEARLY': '${priceIdMap['PLUS:YEARLY']}',`);
  console.log(`    'PRO:MONTHLY': '${priceIdMap['PRO:MONTHLY']}',`);
  console.log(`    'PRO:YEARLY': '${priceIdMap['PRO:YEARLY']}',`);
  console.log(`    'MASTER:MONTHLY': '${priceIdMap['MASTER:MONTHLY']}',`);
  console.log(`    'MASTER:YEARLY': '${priceIdMap['MASTER:YEARLY']}',`);
  console.log('  };');
  console.log('  return map[`${planId}:${period}`];');
  console.log('}\n');

  console.log('💡 TIP: Run this script again to update if you need to change prices.');
  console.log(`💡 For ${mode === 'test' ? 'production' : 'test'} setup, run: node setup-products.js --${mode === 'test' ? 'production' : 'test'}\n`);
}

setupProducts().catch(console.error);
