-- =============================================================================
-- V8 — Assessments
-- AssessmentDefinitions are catalog-like (PLATFORM-managed).
-- AssessmentTests are the individual tests within a definition.
-- AssessmentResults are IMMUTABLE snapshots of a user's measurement.
-- =============================================================================

CREATE TABLE assessment_definitions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    code            VARCHAR(80) NOT NULL UNIQUE,   -- e.g. 'LATTICE_FINGER_STRENGTH_V1'
    target_discipline climbing_discipline,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TRIGGER trg_assessment_definitions_updated
    BEFORE UPDATE ON assessment_definitions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translations: 'name', 'description', 'protocol'

CREATE TABLE assessment_definition_translations (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES assessment_definitions(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (definition_id, locale, field)
);

-- Individual tests within a definition ---------------------------------------

CREATE TABLE assessment_tests (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    definition_id   BIGINT NOT NULL REFERENCES assessment_definitions(id) ON DELETE CASCADE,
    code            VARCHAR(80) NOT NULL,
    position        INTEGER NOT NULL,
    unit            VARCHAR(20) NOT NULL,        -- 'kg','seconds','count','mm','%'
    value_type      VARCHAR(20) NOT NULL,        -- 'INTEGER','DECIMAL'
    UNIQUE (definition_id, code),
    UNIQUE (definition_id, position)
);

CREATE TABLE assessment_test_translations (
    id              BIGSERIAL PRIMARY KEY,
    test_id         BIGINT NOT NULL REFERENCES assessment_tests(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,        -- 'name','description','protocol'
    value           TEXT NOT NULL,
    UNIQUE (test_id, locale, field)
);

-- Results: one row per (user, definition, performed_at) — IMMUTABLE ----------
-- No updated_at on purpose: results are historical records and must not change.

CREATE TABLE assessment_results (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    definition_id   BIGINT NOT NULL REFERENCES assessment_definitions(id),
    performed_at    TIMESTAMPTZ NOT NULL,
    user_weight_kg  NUMERIC(5,2),    -- snapshot of weight at moment of test
    notes           TEXT
);

CREATE INDEX idx_assessment_results_user        ON assessment_results(user_id, performed_at DESC);
CREATE INDEX idx_assessment_results_definition  ON assessment_results(definition_id);

-- Individual measurements per test for a result ------------------------------

CREATE TABLE assessment_metrics (
    id              BIGSERIAL PRIMARY KEY,
    result_id       BIGINT NOT NULL REFERENCES assessment_results(id) ON DELETE CASCADE,
    test_id         BIGINT NOT NULL REFERENCES assessment_tests(id),
    numeric_value   NUMERIC(12,4) NOT NULL,
    UNIQUE (result_id, test_id)
);

CREATE INDEX idx_assessment_metrics_result ON assessment_metrics(result_id);
CREATE INDEX idx_assessment_metrics_test   ON assessment_metrics(test_id);

-- Close the FK declared (without constraint) in V7: AI-generated training
-- plans reference the assessment they were derived from.

ALTER TABLE training_plans
    ADD CONSTRAINT fk_training_plans_generated_from_assessment
    FOREIGN KEY (generated_from_assessment_id)
    REFERENCES assessment_results(id)
    ON DELETE SET NULL;

CREATE INDEX idx_training_plans_generated_from
    ON training_plans(generated_from_assessment_id)
    WHERE generated_from_assessment_id IS NOT NULL;
