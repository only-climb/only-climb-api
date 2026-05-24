-- =============================================================================
-- V4 — Media assets
-- The asset is independent and reusable (can be linked to many exercises,
-- plans, etc. via per-entity join tables). Storage backend is abstracted via
-- (storage_provider, storage_key) so we can swap S3/Cloudinary later.
-- =============================================================================

CREATE TABLE media_assets (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    uploader_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type                media_type NOT NULL,
    storage_provider    storage_provider NOT NULL,
    storage_key         VARCHAR(1024) NOT NULL,
    mime_type           VARCHAR(100) NOT NULL,
    size_bytes          BIGINT,
    width_px            INTEGER,
    height_px           INTEGER,
    duration_seconds    INTEGER,
    checksum_sha256     CHAR(64),
    processing_status   media_processing_status NOT NULL DEFAULT 'READY',
    visibility          content_visibility NOT NULL DEFAULT 'PUBLIC',
    alt_text            VARCHAR(500),     -- fallback alt text (untranslated)

    UNIQUE (storage_provider, storage_key)
);

CREATE INDEX idx_media_assets_uploader  ON media_assets(uploader_id);
CREATE INDEX idx_media_assets_type      ON media_assets(type);
CREATE INDEX idx_media_assets_checksum  ON media_assets(checksum_sha256) WHERE checksum_sha256 IS NOT NULL;
CREATE INDEX idx_media_assets_pending   ON media_assets(id) WHERE processing_status = 'PENDING';

CREATE TRIGGER trg_media_assets_updated
    BEFORE UPDATE ON media_assets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Translations for media assets (only used for PLATFORM-managed media) -------
-- Fields: 'alt_text', 'caption'

CREATE TABLE media_asset_translations (
    id              BIGSERIAL PRIMARY KEY,
    media_id        BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    locale          VARCHAR(10) NOT NULL,
    field           VARCHAR(50) NOT NULL,
    value           TEXT NOT NULL,
    UNIQUE (media_id, locale, field)
);
