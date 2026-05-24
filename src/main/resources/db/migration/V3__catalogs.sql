-- =============================================================================
-- V3 — Catalogs: grades, exercise categories, muscle groups, grip types,
--      parameter types, goal types, equipment.
-- All user-visible catalogs have a companion translations table.
-- =============================================================================

-- Climbing grades (canonical catalog, used as FK from anywhere needing a grade)

CREATE TABLE climbing_grades (
    id          BIGSERIAL PRIMARY KEY,
    scale       grade_scale NOT NULL,
    value       VARCHAR(10) NOT NULL,
    sort_order  INTEGER NOT NULL,
    UNIQUE (scale, value)
);

CREATE INDEX idx_climbing_grades_scale_sort ON climbing_grades(scale, sort_order);

-- Generic helper macro pattern: catalog table + translations -----------------
-- All catalog tables follow: id PK, uuid UNIQUE, code UNIQUE, is_active.
-- Translations: id PK, FK to catalog, locale, field, value, UNIQUE(catalog_id, locale, field)

-- Exercise categories --------------------------------------------------------

CREATE TABLE exercise_categories (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE exercise_category_translations (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES exercise_categories(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (category_id, locale, field)
);

-- Muscle groups --------------------------------------------------------------

CREATE TABLE muscle_groups (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE muscle_group_translations (
    id          BIGSERIAL PRIMARY KEY,
    muscle_group_id BIGINT NOT NULL REFERENCES muscle_groups(id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    field       VARCHAR(50) NOT NULL,
    value       TEXT NOT NULL,
    UNIQUE (muscle_group_id, locale, field)
);

-- Grip types (climbing-specific) ---------------------------------------------

CREATE TABLE grip_types (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE grip_type_translations (
    id          BIGSERIAL PRIMARY KEY,
    grip_type_id BIGINT NOT NULL REFERENCES grip_types(id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    field       VARCHAR(50) NOT NULL,
    value       TEXT NOT NULL,
    UNIQUE (grip_type_id, locale, field)
);

-- Exercise parameter types (REPS, SETS, DURATION_SECONDS, ...) ---------------

CREATE TABLE parameter_types (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    unit        VARCHAR(20),                 -- 'count','seconds','kg','mm','%'
    value_type  VARCHAR(20) NOT NULL,        -- 'INTEGER','DECIMAL','ENUM_GRIP'
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE parameter_type_translations (
    id              BIGSERIAL PRIMARY KEY,
    parameter_type_id BIGINT NOT NULL REFERENCES parameter_types(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (parameter_type_id, locale, field)
);

-- Goal types -----------------------------------------------------------------

CREATE TABLE goal_types (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    requires_target_grade BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE goal_type_translations (
    id          BIGSERIAL PRIMARY KEY,
    goal_type_id BIGINT NOT NULL REFERENCES goal_types(id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    field       VARCHAR(50) NOT NULL,
    value       TEXT NOT NULL,
    UNIQUE (goal_type_id, locale, field)
);

-- Equipment ------------------------------------------------------------------

CREATE TABLE equipment (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE equipment_translations (
    id              BIGSERIAL PRIMARY KEY,
    equipment_id    BIGINT NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (equipment_id, locale, field)
);

-- Seed catalog codes (translations are seeded separately) --------------------

INSERT INTO exercise_categories (code) VALUES
    ('HANGBOARD'), ('PULL'), ('CORE'), ('ANTAGONIST'),
    ('FLEXIBILITY'), ('ENDURANCE'), ('TECHNIQUE');

INSERT INTO muscle_groups (code) VALUES
    ('FINGERS'), ('FOREARM'), ('BACK'), ('SHOULDERS'),
    ('CORE'), ('CHEST'), ('ARMS'), ('LEGS'), ('FULL_BODY');

INSERT INTO grip_types (code) VALUES
    ('CRIMP'), ('HALF_CRIMP'), ('OPEN_HAND'),
    ('PINCH'), ('SLOPER'), ('MONO');

INSERT INTO parameter_types (code, unit, value_type) VALUES
    ('REPS',              'count',   'INTEGER'),
    ('SETS',              'count',   'INTEGER'),
    ('REST_SECONDS',      'seconds', 'INTEGER'),
    ('DURATION_SECONDS',  'seconds', 'INTEGER'),
    ('WEIGHT_KG',         'kg',      'DECIMAL'),
    ('INTENSITY_PERCENT', '%',       'INTEGER'),
    ('EDGE_DEPTH_MM',     'mm',      'INTEGER'),
    ('GRIP_TYPE',         NULL,      'ENUM_GRIP'),
    ('RPE',               'rpe',     'INTEGER');

INSERT INTO goal_types (code, requires_target_grade) VALUES
    ('FINGER_STRENGTH',  FALSE),
    ('POWER_ENDURANCE',  FALSE),
    ('AEROBIC_BASE',     FALSE),
    ('GRADE_TARGET',     TRUE),
    ('ANTAGONIST',       FALSE),
    ('GENERAL_STRENGTH', FALSE);

INSERT INTO equipment (code) VALUES
    ('HANGBOARD'), ('PULLUP_BAR'), ('WEIGHTED_BELT'),
    ('RESISTANCE_BAND'), ('CAMPUS_BOARD'), ('SLINGS'),
    ('MOON_BOARD'), ('KILTER_BOARD'), ('TINDEQ'), ('FOAM_ROLLER');
