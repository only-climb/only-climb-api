-- =============================================================================
-- V7 — Training plans (multi-week programmes)
-- Heavily denormalised metadata for rich filtering / search.
-- Structure:
--   training_plans
--     └─ training_plan_weeks
--           └─ training_plan_sessions (FK to a workout_template)
-- =============================================================================

CREATE TABLE training_plans (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,

    source              content_source NOT NULL,
    generation_type     plan_generation_type NOT NULL DEFAULT 'MANUAL',
    owner_id            BIGINT REFERENCES users(id) ON DELETE SET NULL,
    forked_from_id      BIGINT REFERENCES training_plans(id) ON DELETE SET NULL,
    visibility          content_visibility NOT NULL DEFAULT 'PRIVATE',

    -- Structured filterable metadata -----------------------------------------
    difficulty_level         difficulty_level NOT NULL,
    target_discipline        climbing_discipline NOT NULL,
    primary_goal_type_id     BIGINT NOT NULL REFERENCES goal_types(id),
    target_grade_min_id      BIGINT REFERENCES climbing_grades(id),
    target_grade_max_id      BIGINT REFERENCES climbing_grades(id),

    duration_weeks           INTEGER NOT NULL CHECK (duration_weeks BETWEEN 1 AND 104),
    sessions_per_week        INTEGER NOT NULL CHECK (sessions_per_week BETWEEN 1 AND 14),
    avg_session_duration_minutes INTEGER CHECK (avg_session_duration_minutes IS NULL OR avg_session_duration_minutes BETWEEN 5 AND 600),
    training_volume          training_volume NOT NULL DEFAULT 'MODERATE',

    -- Quick boolean filters for common search needs --------------------------
    requires_hangboard          BOOLEAN NOT NULL DEFAULT FALSE,
    requires_campus_board       BOOLEAN NOT NULL DEFAULT FALSE,
    requires_gym_access         BOOLEAN NOT NULL DEFAULT FALSE,
    requires_outdoor_climbing   BOOLEAN NOT NULL DEFAULT FALSE,
    is_recovery_focused         BOOLEAN NOT NULL DEFAULT FALSE,

    -- Engagement metrics (denormalised counters, recomputed by background job)
    forks_count                 INTEGER NOT NULL DEFAULT 0,
    completions_count           INTEGER NOT NULL DEFAULT 0,

    -- AI generation metadata (populated only when generation_type = AI_GENERATED)
    ai_model                    VARCHAR(80),
    ai_prompt_version           VARCHAR(40),
    generated_from_assessment_id BIGINT,   -- FK added in V8 (assessments)

    CONSTRAINT training_plans_owner_consistency CHECK (
        (source = 'PLATFORM'     AND owner_id IS NULL) OR
        (source = 'USER_CREATED' AND owner_id IS NOT NULL)
    ),
    CONSTRAINT training_plans_platform_is_public CHECK (
        source = 'USER_CREATED' OR visibility = 'PUBLIC'
    ),
    CONSTRAINT training_plans_grade_range CHECK (
        target_grade_min_id IS NULL
        OR target_grade_max_id IS NULL
        OR target_grade_min_id <= target_grade_max_id
    ),
    CONSTRAINT training_plans_ai_metadata CHECK (
        generation_type <> 'AI_GENERATED'
        OR (ai_model IS NOT NULL AND generated_from_assessment_id IS NOT NULL)
    )
);

CREATE INDEX idx_training_plans_source         ON training_plans(source);
CREATE INDEX idx_training_plans_owner          ON training_plans(owner_id);
CREATE INDEX idx_training_plans_visibility     ON training_plans(visibility);
CREATE INDEX idx_training_plans_difficulty     ON training_plans(difficulty_level);
CREATE INDEX idx_training_plans_discipline     ON training_plans(target_discipline);
CREATE INDEX idx_training_plans_primary_goal   ON training_plans(primary_goal_type_id);
CREATE INDEX idx_training_plans_duration       ON training_plans(duration_weeks);
CREATE INDEX idx_training_plans_volume         ON training_plans(training_volume);
CREATE INDEX idx_training_plans_grade_range    ON training_plans(target_grade_min_id, target_grade_max_id);
CREATE INDEX idx_training_plans_published      ON training_plans(published_at DESC)
    WHERE deleted_at IS NULL AND visibility = 'PUBLIC';

CREATE TRIGGER trg_training_plans_updated
    BEFORE UPDATE ON training_plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translations (rich content for filtering by language) ----------------------
-- fields: 'name', 'short_description', 'description', 'methodology',
--         'prerequisites', 'expected_outcomes', 'author_notes', 'coaching_tips'

CREATE TABLE training_plan_translations (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (plan_id, locale, field)
);

CREATE INDEX idx_training_plan_translations_lookup ON training_plan_translations(plan_id, locale);

-- Secondary goals (many-to-many) ---------------------------------------------

CREATE TABLE training_plan_secondary_goals (
    plan_id      BIGINT NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    goal_type_id BIGINT NOT NULL REFERENCES goal_types(id),
    PRIMARY KEY (plan_id, goal_type_id)
);

-- Equipment required/optional for the plan -----------------------------------

CREATE TABLE training_plan_equipment (
    plan_id      BIGINT NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    is_optional  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (plan_id, equipment_id)
);

-- Weeks ----------------------------------------------------------------------

CREATE TABLE training_plan_weeks (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    plan_id         BIGINT NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    week_number     INTEGER NOT NULL CHECK (week_number >= 1),
    is_deload       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (plan_id, week_number)
);

CREATE INDEX idx_training_plan_weeks_plan ON training_plan_weeks(plan_id);

-- Translations: 'name', 'focus', 'notes'

CREATE TABLE training_plan_week_translations (
    id              BIGSERIAL PRIMARY KEY,
    week_id         BIGINT NOT NULL REFERENCES training_plan_weeks(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (week_id, locale, field)
);

-- Sessions inside a week (point to a workout_template) -----------------------

CREATE TABLE training_plan_sessions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    week_id         BIGINT NOT NULL REFERENCES training_plan_weeks(id) ON DELETE CASCADE,
    day_of_week     INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    position        INTEGER NOT NULL DEFAULT 1,
    workout_template_id BIGINT NOT NULL REFERENCES workout_templates(id),
    is_optional     BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (week_id, day_of_week, position)
);

CREATE INDEX idx_training_plan_sessions_week     ON training_plan_sessions(week_id);
CREATE INDEX idx_training_plan_sessions_template ON training_plan_sessions(workout_template_id);

-- Translations: 'notes'

CREATE TABLE training_plan_session_translations (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES training_plan_sessions(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (session_id, locale, field)
);
