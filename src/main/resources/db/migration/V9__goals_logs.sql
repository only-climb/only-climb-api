-- =============================================================================
-- V9 — User goals and workout logs
-- =============================================================================

CREATE TABLE user_goals (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_type_id    BIGINT NOT NULL REFERENCES goal_types(id),
    target_grade_id BIGINT REFERENCES climbing_grades(id),
    target_date     DATE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    achieved_at     TIMESTAMPTZ,
    notes           TEXT
);

CREATE INDEX idx_user_goals_user_active ON user_goals(user_id) WHERE is_active = TRUE;
CREATE UNIQUE INDEX idx_user_goals_one_active_per_user
    ON user_goals(user_id) WHERE is_active = TRUE;

CREATE TRIGGER trg_user_goals_updated
    BEFORE UPDATE ON user_goals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Workout logs (one per session a user actually performed) -------------------

CREATE TABLE workout_logs (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workout_template_id BIGINT REFERENCES workout_templates(id) ON DELETE SET NULL,
    plan_session_id     BIGINT REFERENCES training_plan_sessions(id) ON DELETE SET NULL,
    performed_at        TIMESTAMPTZ NOT NULL,
    duration_minutes    INTEGER,
    perceived_effort    INTEGER CHECK (perceived_effort IS NULL OR perceived_effort BETWEEN 1 AND 10),
    notes               TEXT
);

CREATE INDEX idx_workout_logs_user       ON workout_logs(user_id, performed_at DESC);
CREATE INDEX idx_workout_logs_template   ON workout_logs(workout_template_id);
CREATE INDEX idx_workout_logs_plan_sess  ON workout_logs(plan_session_id);

CREATE TRIGGER trg_workout_logs_updated
    BEFORE UPDATE ON workout_logs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Entries (one row per exercise in the logged workout) -----------------------
-- planned_config: snapshot of the template's config at execution time
-- actual_config : what the user actually did

CREATE TABLE workout_log_entries (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    log_id          BIGINT NOT NULL REFERENCES workout_logs(id) ON DELETE CASCADE,
    exercise_id     BIGINT NOT NULL REFERENCES exercises(id),
    position        INTEGER NOT NULL,
    status          workout_log_entry_status NOT NULL DEFAULT 'COMPLETED',
    planned_config  JSONB NOT NULL DEFAULT '{}'::jsonb,
    actual_config   JSONB NOT NULL DEFAULT '{}'::jsonb,
    notes           TEXT,
    UNIQUE (log_id, position)
);

CREATE INDEX idx_workout_log_entries_log      ON workout_log_entries(log_id);
CREATE INDEX idx_workout_log_entries_exercise ON workout_log_entries(exercise_id);
CREATE INDEX idx_workout_log_entries_actual   ON workout_log_entries USING GIN (actual_config);
