-- =============================================================================
-- V11 — AI plan generation jobs
-- =============================================================================
-- Tracks asynchronous AI plan generations. LLM calls are slow (10-60s) and
-- must not block HTTP requests; jobs are processed by a worker and the
-- client polls (or subscribes via SSE) for the result.
--
-- Design goals:
--   * Provider-agnostic: today OpenAI, tomorrow Anthropic / local models.
--     Provider + model + prompt_version are stored, never hard-coded.
--   * Auditable: every billable call records token usage and cost.
--   * Recoverable: a failed job can be retried; we link the retry chain.
--   * Quota-aware: easy to count jobs per user per billing period.
--   * Simple: a single flat table, no extra join tables. Inputs travel as a
--     JSONB snapshot so the prompt builder is independent from current
--     user / goal / assessment state at the moment the job ran.
-- =============================================================================

CREATE TABLE ai_plan_generation_jobs (
    id                       BIGSERIAL PRIMARY KEY,
    uuid                     UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Who requested it -------------------------------------------------------
    user_id                  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- Subscription that authorised this call (premium gate audit trail).
    -- Nullable: keep the job history even if the subscription row is later
    -- recreated on plan change.
    subscription_id          BIGINT REFERENCES user_subscriptions(id) ON DELETE SET NULL,

    -- Domain inputs ----------------------------------------------------------
    assessment_result_id     BIGINT NOT NULL REFERENCES assessment_results(id) ON DELETE RESTRICT,
    goal_id                  BIGINT NOT NULL REFERENCES user_goals(id) ON DELETE RESTRICT,
    -- Frozen snapshot of all inputs sent to the model: schedule constraints,
    -- assessment metrics, goal payload, user profile bits (weight, locale...).
    -- Stored so we can reproduce / debug a generation independently from
    -- mutable rows in users/profiles.
    input_payload            JSONB NOT NULL,

    -- Provider configuration -------------------------------------------------
    ai_provider              ai_provider NOT NULL,
    model                    VARCHAR(100) NOT NULL,     -- e.g. 'gpt-4o-2024-08-06'
    prompt_version           VARCHAR(50)  NOT NULL,     -- e.g. 'v3', for A/B + reproducibility
    -- External request id returned by the provider (OpenAI's response id,
    -- etc.). Optional — only set once the call is dispatched.
    external_request_id      VARCHAR(255),

    -- Execution lifecycle ----------------------------------------------------
    status                   ai_job_status NOT NULL DEFAULT 'PENDING',
    queued_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at               TIMESTAMPTZ,
    finished_at              TIMESTAMPTZ,
    duration_ms              INTEGER,                   -- finished_at - started_at, denormalised for fast aggregation

    -- Usage & cost (filled when the provider reports them) -------------------
    prompt_tokens            INTEGER,
    completion_tokens        INTEGER,
    total_tokens             INTEGER,
    -- Cost in micros of the plan currency (1e-6). Avoids floats and lets us
    -- store sub-cent OpenAI prices accurately.
    cost_micros              BIGINT,
    cost_currency            CHAR(3),

    -- Outcome ----------------------------------------------------------------
    -- The generated plan (set when status = SUCCEEDED). ON DELETE SET NULL so
    -- deleting the plan keeps the audit trail.
    resulting_plan_id        BIGINT REFERENCES training_plans(id) ON DELETE SET NULL,
    -- Raw / structured response from the provider, kept for debugging and
    -- regeneration without re-billing.
    raw_response             JSONB,

    -- Failure handling -------------------------------------------------------
    error_code               VARCHAR(100),
    error_message            TEXT,
    retry_of_job_id          BIGINT REFERENCES ai_plan_generation_jobs(id) ON DELETE SET NULL,
    attempt_count            INTEGER NOT NULL DEFAULT 1 CHECK (attempt_count >= 1),

    -- Invariants -------------------------------------------------------------
    CONSTRAINT ai_jobs_terminal_has_finished_at CHECK (
        (status IN ('SUCCEEDED', 'FAILED', 'CANCELLED') AND finished_at IS NOT NULL)
        OR (status IN ('PENDING', 'RUNNING') AND finished_at IS NULL)
    ),
    CONSTRAINT ai_jobs_success_has_plan CHECK (
        status <> 'SUCCEEDED' OR resulting_plan_id IS NOT NULL
    ),
    CONSTRAINT ai_jobs_failure_has_error CHECK (
        status <> 'FAILED' OR error_code IS NOT NULL
    ),
    CONSTRAINT ai_jobs_started_before_finished CHECK (
        started_at IS NULL OR finished_at IS NULL OR finished_at >= started_at
    )
);

CREATE INDEX idx_ai_jobs_user_created   ON ai_plan_generation_jobs(user_id, created_at DESC);
CREATE INDEX idx_ai_jobs_status         ON ai_plan_generation_jobs(status);
CREATE INDEX idx_ai_jobs_queue          ON ai_plan_generation_jobs(queued_at)
    WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX idx_ai_jobs_provider_model ON ai_plan_generation_jobs(ai_provider, model);
CREATE INDEX idx_ai_jobs_resulting_plan ON ai_plan_generation_jobs(resulting_plan_id)
    WHERE resulting_plan_id IS NOT NULL;
-- One pending/running job per user — prevents accidental double-spends.
CREATE UNIQUE INDEX idx_ai_jobs_one_inflight_per_user
    ON ai_plan_generation_jobs(user_id)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE TRIGGER trg_ai_plan_generation_jobs_updated
    BEFORE UPDATE ON ai_plan_generation_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
