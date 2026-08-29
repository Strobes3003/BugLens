CREATE TABLE releases (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    release_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_releases_project_version UNIQUE (project_id, version),
    CONSTRAINT ck_releases_status CHECK (status IN ('PLANNED', 'RELEASED', 'ARCHIVED'))
);

CREATE INDEX idx_releases_project_id ON releases (project_id);
CREATE INDEX idx_releases_status ON releases (status);
CREATE UNIQUE INDEX uk_releases_project_version_lower ON releases (project_id, LOWER(version));
