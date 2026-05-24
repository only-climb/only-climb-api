-- =============================================================================
-- V5 — Exercises and their relationships (media, equipment, parameters,
--      muscle groups). Translatable text lives in exercise_translations.
-- =============================================================================

CREATE TABLE exercises (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    source          content_source NOT NULL,
    owner_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    category_id     BIGINT NOT NULL REFERENCES exercise_categories(id),
    visibility      content_visibility NOT NULL DEFAULT 'PRIVATE',

    -- Structured (non-translatable) metadata for filtering & search
    difficulty_level         difficulty_level NOT NULL DEFAULT 'BEGINNER',
    primary_muscle_group_id  BIGINT NOT NULL REFERENCES muscle_groups(id),
    requires_equipment       BOOLEAN NOT NULL DEFAULT FALSE,
    is_unilateral            BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_duration_minutes INTEGER,
    safety_warning_level     safety_warning_level NOT NULL DEFAULT 'NONE',

    CONSTRAINT exercises_owner_consistency CHECK (
        (source = 'PLATFORM'     AND owner_id IS NULL) OR
        (source = 'USER_CREATED' AND owner_id IS NOT NULL)
    ),
    CONSTRAINT exercises_platform_is_public CHECK (
        source = 'USER_CREATED' OR visibility = 'PUBLIC'
    )
);

CREATE INDEX idx_exercises_source           ON exercises(source);
CREATE INDEX idx_exercises_owner            ON exercises(owner_id);
CREATE INDEX idx_exercises_category         ON exercises(category_id);
CREATE INDEX idx_exercises_visibility       ON exercises(visibility);
CREATE INDEX idx_exercises_difficulty       ON exercises(difficulty_level);
CREATE INDEX idx_exercises_primary_muscle   ON exercises(primary_muscle_group_id);
CREATE INDEX idx_exercises_not_deleted      ON exercises(id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_exercises_updated
    BEFORE UPDATE ON exercises
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translatable content for exercises -----------------------------------------
-- field values: 'name', 'short_description', 'description',
--               'execution_instructions', 'common_mistakes',
--               'progressions', 'safety_notes'

CREATE TABLE exercise_translations (
    id              BIGSERIAL PRIMARY KEY,
    exercise_id     BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (exercise_id, locale, field)
);

CREATE INDEX idx_exercise_translations_lookup
    ON exercise_translations(exercise_id, locale);

-- Secondary muscle groups (many-to-many) -------------------------------------

CREATE TABLE exercise_secondary_muscles (
    exercise_id      BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    muscle_group_id  BIGINT NOT NULL REFERENCES muscle_groups(id),
    PRIMARY KEY (exercise_id, muscle_group_id)
);

-- Allowed parameters for each exercise ---------------------------------------
-- Defines which parameters are valid when configuring this exercise in a
-- workout template. Validation of values is enforced in application code.

CREATE TABLE exercise_allowed_parameters (
    exercise_id       BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    parameter_type_id BIGINT NOT NULL REFERENCES parameter_types(id),
    is_required       BOOLEAN NOT NULL DEFAULT FALSE,
    default_value     VARCHAR(50),
    PRIMARY KEY (exercise_id, parameter_type_id)
);

-- Equipment required/optional for the exercise -------------------------------

CREATE TABLE exercise_equipment (
    exercise_id  BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    is_optional  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (exercise_id, equipment_id)
);

-- Media gallery for the exercise ---------------------------------------------

CREATE TABLE exercise_media (
    id           BIGSERIAL PRIMARY KEY,
    exercise_id  BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    media_id     BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    role         media_role NOT NULL,
    position     INTEGER NOT NULL,
    UNIQUE (exercise_id, media_id),
    UNIQUE (exercise_id, position)
);

CREATE INDEX idx_exercise_media_exercise ON exercise_media(exercise_id);
