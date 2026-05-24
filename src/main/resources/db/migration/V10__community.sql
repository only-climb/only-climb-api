-- =============================================================================
-- V10 — Community: training groups and user follow graph
-- =============================================================================

CREATE TABLE training_groups (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_plan_id  BIGINT REFERENCES training_plans(id) ON DELETE SET NULL,
    slug            VARCHAR(80) NOT NULL UNIQUE,
    visibility      group_visibility NOT NULL DEFAULT 'PUBLIC',
    members_count   INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_training_groups_owner       ON training_groups(owner_id);
CREATE INDEX idx_training_groups_shared_plan ON training_groups(shared_plan_id)
    WHERE shared_plan_id IS NOT NULL;

CREATE TRIGGER trg_training_groups_updated
    BEFORE UPDATE ON training_groups
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translations: 'name', 'description'

CREATE TABLE training_group_translations (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT NOT NULL REFERENCES training_groups(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (group_id, locale, field)
);

-- Membership ------------------------------------------------------------------

CREATE TABLE training_group_members (
    group_id        BIGINT NOT NULL REFERENCES training_groups(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            group_member_role NOT NULL DEFAULT 'MEMBER',
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_training_group_members_user ON training_group_members(user_id);

-- User follow graph -----------------------------------------------------------

CREATE TABLE user_followers (
    follower_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT user_followers_no_self CHECK (follower_id <> following_id)
);

CREATE INDEX idx_user_followers_following ON user_followers(following_id);
