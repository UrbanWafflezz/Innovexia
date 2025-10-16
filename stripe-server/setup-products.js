import 'dotenv/config';
import Stripe from 'stripe';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, { apiVersion: '2024-06-20' });

async function setupProducts() {
  console.log('🚀 Creating Stripe products and prices...\n');

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
      // Create product
      const product = await stripe.products.create({
        name: productData.name,
        description: productData.description,
      });

      console.log(`✅ Created product: ${product.name} (${product.id})`);

      // Create prices for this product
      for (const priceData of productData.prices) {
        const price = await stripe.prices.create({
          product: product.id,
          unit_amount: priceData.amount,
          currency: 'usd',
          recurring: {
            interval: priceData.interval
          }
        });

        priceIdMap[priceData.key] = price.id;
        const displayAmount = (priceData.amount / 100).toFixed(2);
        console.log(`  ✅ Created ${priceData.interval}ly price: $${displayAmount} (${price.id})`);
      }

      console.log('');
    } catch (error) {
      console.error(`❌ Error creating ${productData.name}:`, error.message);
    }
  }

  console.log('\n📋 Price ID Mapping:');
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
}

setupProducts().catch(console.error);
