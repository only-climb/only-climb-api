-- =============================================================================
-- V1 — Extensions and shared ENUM types
-- =============================================================================
-- Convention: every table has (id BIGSERIAL PK, uuid UUID UNIQUE NOT NULL,
--   created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ).
-- UUIDs are the public identity; the Long PK is internal to JPA.
-- ENUMs are used for closed control flags only. User-visible concepts use
-- catalog tables with translations.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- Generic enums --------------------------------------------------------------

CREATE TYPE content_source AS ENUM ('PLATFORM', 'USER_CREATED');
CREATE TYPE content_visibility AS ENUM ('PRIVATE', 'PUBLIC');
CREATE TYPE group_visibility AS ENUM ('PUBLIC', 'INVITE_ONLY');
CREATE TYPE difficulty_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ELITE');
CREATE TYPE training_volume AS ENUM ('LOW', 'MODERATE', 'HIGH');
CREATE TYPE safety_warning_level AS ENUM ('NONE', 'MODERATE', 'HIGH');

-- Climbing-specific enums ----------------------------------------------------

CREATE TYPE climbing_discipline AS ENUM ('SPORT', 'BOULDER', 'TRAD');
CREATE TYPE grade_scale AS ENUM ('FRENCH', 'FONTAINEBLEAU');

-- User / subscription enums --------------------------------------------------

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE auth_provider AS ENUM ('CLERK');
CREATE TYPE payment_provider AS ENUM ('STRIPE');
CREATE TYPE billing_period AS ENUM ('MONTHLY', 'YEARLY', 'LIFETIME');
CREATE TYPE subscription_status AS ENUM (
    'TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED'
);
CREATE TYPE invoice_status AS ENUM (
    'DRAFT', 'OPEN', 'PAID', 'UNCOLLECTIBLE', 'VOID', 'REFUNDED'
);

-- Media enums ----------------------------------------------------------------

CREATE TYPE media_type AS ENUM ('IMAGE', 'VIDEO', 'DOCUMENT');
CREATE TYPE storage_provider AS ENUM ('S3', 'CLOUDINARY', 'EXTERNAL_URL');
CREATE TYPE media_processing_status AS ENUM ('PENDING', 'READY', 'FAILED');
CREATE TYPE media_role AS ENUM (
    'THUMBNAIL', 'DEMONSTRATION', 'TECHNIQUE_DETAIL',
    'COMMON_MISTAKE', 'REFERENCE'
);

-- Plan / log enums -----------------------------------------------------------

CREATE TYPE plan_generation_type AS ENUM ('MANUAL', 'AI_GENERATED', 'FORKED');
CREATE TYPE workout_log_entry_status AS ENUM ('COMPLETED', 'SKIPPED', 'MODIFIED');

-- AI enums -------------------------------------------------------------------

CREATE TYPE ai_provider AS ENUM ('OPENAI');
CREATE TYPE ai_job_status AS ENUM (
    'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
);

-- Group enums ----------------------------------------------------------------

CREATE TYPE group_member_role AS ENUM ('ADMIN', 'MEMBER');

-- Reusable trigger to maintain updated_at automatically ----------------------

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
