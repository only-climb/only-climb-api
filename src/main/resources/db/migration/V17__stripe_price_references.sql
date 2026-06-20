-- =============================================================================
-- V17 — Stripe Price references
-- =============================================================================
-- Links the seeded subscription_plans with their corresponding Stripe Price
-- IDs created in the Stripe Dashboard (test mode).
-- Idempotent: only sets external_ref when currently NULL.

-- BASIC monthly (4.99€)
UPDATE subscription_plans
   SET external_ref = 'price_1TkKi1RNtxbyuSNRybBVSf5Q'
 WHERE external_ref IS NULL
   AND billing_period = 'MONTHLY'
   AND tier_id = (SELECT id FROM subscription_tiers WHERE code = 'BASIC');

-- BASIC yearly (49.90€)
UPDATE subscription_plans
   SET external_ref = 'price_1TkKibRNtxbyuSNRj7EXoOpq'
 WHERE external_ref IS NULL
   AND billing_period = 'YEARLY'
   AND tier_id = (SELECT id FROM subscription_tiers WHERE code = 'BASIC');

-- PREMIUM monthly (9.99€)
UPDATE subscription_plans
   SET external_ref = 'price_1TkKioRNtxbyuSNRIlqe8Hns'
 WHERE external_ref IS NULL
   AND billing_period = 'MONTHLY'
   AND tier_id = (SELECT id FROM subscription_tiers WHERE code = 'PREMIUM');

-- PREMIUM yearly (99.90€)
UPDATE subscription_plans
   SET external_ref = 'price_1TkKipRNtxbyuSNRcYVZZ9KP'
 WHERE external_ref IS NULL
   AND billing_period = 'YEARLY'
   AND tier_id = (SELECT id FROM subscription_tiers WHERE code = 'PREMIUM');
