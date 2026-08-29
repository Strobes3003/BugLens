CREATE TABLE components (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_components_project_name UNIQUE (project_id, name),
    CONSTRAINT ck_components_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_components_project_id ON components (project_id);
CREATE INDEX idx_components_status ON components (status);
CREATE UNIQUE INDEX uk_components_project_name_lower ON components (project_id, LOWER(name));
