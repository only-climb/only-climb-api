-- =============================================================================
-- V2 — Users, profiles and subscriptions
-- =============================================================================

-- Users ----------------------------------------------------------------------
-- Identity is owned by an external auth provider (Clerk today, others later).
-- We never store passwords. The pair (auth_provider, external_user_id) is the
-- stable foreign identity. Email is kept in our DB for lookups / notifications.

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,

    auth_provider       auth_provider NOT NULL DEFAULT 'CLERK',
    external_user_id    VARCHAR(255)  NOT NULL,
    email               VARCHAR(320)  NOT NULL,
    role                user_role     NOT NULL DEFAULT 'USER',
    last_login_at       TIMESTAMPTZ,

    UNIQUE (auth_provider, external_user_id)
);

-- Case-insensitive uniqueness for email
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_not_deleted ON users(id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- User profile (1:1 with users) ----------------------------------------------

CREATE TABLE user_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name        VARCHAR(100),
    weight_kg           NUMERIC(5,2) CHECK (weight_kg IS NULL OR weight_kg BETWEEN 20 AND 250),
    height_cm           INTEGER      CHECK (height_cm IS NULL OR height_cm BETWEEN 80 AND 250),
    primary_discipline  climbing_discipline,
    locale              VARCHAR(10)  NOT NULL DEFAULT 'en'
);

CREATE TRIGGER trg_user_profiles_updated
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================================
-- Subscription model
--   subscription_tiers   — catalog of product tiers (FREE, BASIC, PREMIUM, ...)
--   subscription_plans   — concrete SKUs (tier × billing_period × currency)
--   user_subscriptions   — a user's current subscription state for a plan
-- The tier catalog is i18n-aware (marketing copy). Plans are not translated.
-- =============================================================================

CREATE TABLE subscription_tiers (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Translations: 'name', 'tagline', 'description'
CREATE TABLE subscription_tier_translations (
    id          BIGSERIAL PRIMARY KEY,
    tier_id     BIGINT NOT NULL REFERENCES subscription_tiers(id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    field       VARCHAR(50) NOT NULL,
    value       TEXT NOT NULL,
    UNIQUE (tier_id, locale, field)
);

CREATE TABLE subscription_plans (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    tier_id         BIGINT NOT NULL REFERENCES subscription_tiers(id),
    billing_period  billing_period NOT NULL,
    price_cents     INTEGER NOT NULL CHECK (price_cents >= 0),
    currency        CHAR(3) NOT NULL DEFAULT 'EUR',
    external_ref    VARCHAR(255),                 -- Stripe price id, etc.
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    UNIQUE (tier_id, billing_period, currency)
);

CREATE INDEX idx_subscription_plans_tier ON subscription_plans(tier_id);

CREATE TRIGGER trg_subscription_plans_updated
    BEFORE UPDATE ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Seed tiers (translations are loaded by a separate seeder / admin task) ------

INSERT INTO subscription_tiers (code, sort_order) VALUES
    ('FREE',    0),
    ('BASIC',   10),
    ('PREMIUM', 20);

-- Seed plans -----------------------------------------------------------------

INSERT INTO subscription_plans (tier_id, billing_period, price_cents, currency)
SELECT id, 'LIFETIME', 0, 'EUR' FROM subscription_tiers WHERE code = 'FREE';

INSERT INTO subscription_plans (tier_id, billing_period, price_cents, currency)
SELECT id, 'MONTHLY',  499, 'EUR' FROM subscription_tiers WHERE code = 'BASIC';
INSERT INTO subscription_plans (tier_id, billing_period, price_cents, currency)
SELECT id, 'YEARLY',  4990, 'EUR' FROM subscription_tiers WHERE code = 'BASIC';

INSERT INTO subscription_plans (tier_id, billing_period, price_cents, currency)
SELECT id, 'MONTHLY',  999, 'EUR' FROM subscription_tiers WHERE code = 'PREMIUM';
INSERT INTO subscription_plans (tier_id, billing_period, price_cents, currency)
SELECT id, 'YEARLY',  9990, 'EUR' FROM subscription_tiers WHERE code = 'PREMIUM';

-- =============================================================================
-- Payment customers
--   Canonical mapping of (user, payment_provider) -> external customer id.
--   Survives plan changes and re-subscriptions, so we never lose the Stripe
--   customer pointer for a user.
-- =============================================================================

CREATE TABLE payment_customers (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_provider        payment_provider NOT NULL,
    external_customer_id    VARCHAR(255) NOT NULL,

    UNIQUE (user_id, payment_provider),
    UNIQUE (payment_provider, external_customer_id)
);

CREATE TRIGGER trg_payment_customers_updated
    BEFORE UPDATE ON payment_customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- User subscriptions ---------------------------------------------------------
-- One row per logical subscription cycle. Renewals update current_period_*;
-- plan changes create a new row (the old one is set to CANCELLED).
-- payment_provider is NULL for the internal free plan (no gateway involved).
-- When set, external_subscription_id is required.

CREATE TABLE user_subscriptions (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id                     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id                     BIGINT NOT NULL REFERENCES subscription_plans(id),
    status                      subscription_status NOT NULL,

    current_period_start        TIMESTAMPTZ NOT NULL,
    current_period_end          TIMESTAMPTZ,
    trial_ends_at               TIMESTAMPTZ,
    cancel_at_period_end        BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at                TIMESTAMPTZ,

    payment_provider            payment_provider,
    external_subscription_id    VARCHAR(255),

    CONSTRAINT user_subscriptions_provider_ref CHECK (
        (payment_provider IS NULL     AND external_subscription_id IS NULL) OR
        (payment_provider IS NOT NULL AND external_subscription_id IS NOT NULL)
    )
);

CREATE INDEX idx_user_subscriptions_user   ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_plan   ON user_subscriptions(plan_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);
CREATE UNIQUE INDEX idx_user_subscriptions_one_active
    ON user_subscriptions(user_id)
    WHERE status IN ('ACTIVE', 'TRIALING', 'PAST_DUE');
CREATE UNIQUE INDEX idx_user_subscriptions_provider_ref
    ON user_subscriptions(payment_provider, external_subscription_id)
    WHERE payment_provider IS NOT NULL;

CREATE TRIGGER trg_user_subscriptions_updated
    BEFORE UPDATE ON user_subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================================
-- Subscription invoices
--   Historical billing records. Driven by webhooks from the payment provider.
--   Used to render "billing history" and to track refunds.
-- =============================================================================

CREATE TABLE subscription_invoices (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id         BIGINT REFERENCES user_subscriptions(id) ON DELETE SET NULL,
    payment_provider        payment_provider NOT NULL,
    external_invoice_id     VARCHAR(255) NOT NULL,

    status                  invoice_status NOT NULL,
    amount_cents            INTEGER NOT NULL CHECK (amount_cents >= 0),
    amount_refunded_cents   INTEGER NOT NULL DEFAULT 0 CHECK (amount_refunded_cents >= 0),
    currency                CHAR(3) NOT NULL,

    period_start            TIMESTAMPTZ,
    period_end              TIMESTAMPTZ,
    issued_at               TIMESTAMPTZ,
    paid_at                 TIMESTAMPTZ,

    invoice_pdf_url         VARCHAR(1024),
    hosted_invoice_url      VARCHAR(1024),

    CONSTRAINT subscription_invoices_refund_bound
        CHECK (amount_refunded_cents <= amount_cents),
    UNIQUE (payment_provider, external_invoice_id)
);

CREATE INDEX idx_subscription_invoices_user   ON subscription_invoices(user_id, issued_at DESC);
CREATE INDEX idx_subscription_invoices_sub    ON subscription_invoices(subscription_id);
CREATE INDEX idx_subscription_invoices_status ON subscription_invoices(status);

CREATE TRIGGER trg_subscription_invoices_updated
    BEFORE UPDATE ON subscription_invoices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================================
-- Payment webhook events
--   Inbound events from the payment provider. The UNIQUE constraint on
--   (provider, external_event_id) gives us at-most-once processing semantics:
--   a duplicated webhook delivery fails to insert and we can safely no-op.
-- =============================================================================

CREATE TABLE payment_webhook_events (
    id                  BIGSERIAL PRIMARY KEY,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    payment_provider    payment_provider NOT NULL,
    external_event_id   VARCHAR(255) NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,

    processed_at        TIMESTAMPTZ,
    processing_error    TEXT,

    UNIQUE (payment_provider, external_event_id)
);

CREATE INDEX idx_payment_webhook_events_unprocessed
    ON payment_webhook_events(received_at)
    WHERE processed_at IS NULL;
