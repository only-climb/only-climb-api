-- =============================================================================
-- V6 — Workout templates (training sessions)
-- A template is an ordered list of exercises with their per-exercise config
-- stored as JSONB. Validation of the JSONB against the exercise's allowed
-- parameters happens in application code.
-- =============================================================================

CREATE TABLE workout_templates (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    source          content_source NOT NULL,
    owner_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    forked_from_id  BIGINT REFERENCES workout_templates(id) ON DELETE SET NULL,
    visibility      content_visibility NOT NULL DEFAULT 'PRIVATE',

    difficulty_level            difficulty_level NOT NULL DEFAULT 'BEGINNER',
    estimated_duration_minutes  INTEGER,
    target_discipline           climbing_discipline,

    CONSTRAINT workout_templates_owner_consistency CHECK (
        (source = 'PLATFORM'     AND owner_id IS NULL) OR
        (source = 'USER_CREATED' AND owner_id IS NOT NULL)
    ),
    CONSTRAINT workout_templates_platform_is_public CHECK (
        source = 'USER_CREATED' OR visibility = 'PUBLIC'
    )
);

CREATE INDEX idx_workout_templates_source     ON workout_templates(source);
CREATE INDEX idx_workout_templates_owner      ON workout_templates(owner_id);
CREATE INDEX idx_workout_templates_visibility ON workout_templates(visibility);
CREATE INDEX idx_workout_templates_discipline ON workout_templates(target_discipline);
CREATE INDEX idx_workout_templates_difficulty ON workout_templates(difficulty_level);
CREATE INDEX idx_workout_templates_not_deleted ON workout_templates(id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_workout_templates_updated
    BEFORE UPDATE ON workout_templates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translations: 'name', 'description', 'coach_notes'

CREATE TABLE workout_template_translations (
    id              BIGSERIAL PRIMARY KEY,
    template_id     BIGINT NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (template_id, locale, field)
);

-- Exercises inside the template, ordered, with their config ------------------

CREATE TABLE workout_template_exercises (
    id              BIGSERIAL PRIMARY KEY,
    template_id     BIGINT NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    exercise_id     BIGINT NOT NULL REFERENCES exercises(id),
    position        INTEGER NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (template_id, position)
);

CREATE INDEX idx_workout_template_exercises_template ON workout_template_exercises(template_id);
CREATE INDEX idx_workout_template_exercises_exercise ON workout_template_exercises(exercise_id);
CREATE INDEX idx_workout_template_exercises_config   ON workout_template_exercises USING GIN (config);

-- Per-exercise notes inside a template (translatable) ------------------------

CREATE TABLE workout_template_exercise_translations (
    id              BIGSERIAL PRIMARY KEY,
    template_exercise_id BIGINT NOT NULL REFERENCES workout_template_exercises(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,    -- 'notes'
    value           TEXT NOT NULL,
    UNIQUE (template_exercise_id, locale, field)
);
