UPDATE releases
SET status = 'RELEASED'
WHERE status = 'ARCHIVED';

ALTER TABLE releases DROP CONSTRAINT IF EXISTS ck_releases_status;
ALTER TABLE releases DROP CONSTRAINT IF EXISTS uk_releases_project_version;
DROP INDEX IF EXISTS uk_releases_project_version_lower;

ALTER TABLE releases DROP COLUMN IF EXISTS version;
ALTER TABLE releases RENAME COLUMN release_date TO target_date;

ALTER TABLE releases
    ADD CONSTRAINT ck_releases_status
    CHECK (status IN ('PLANNED', 'ACTIVE', 'RELEASED'));
