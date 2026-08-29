CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    issue_key VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    component_id BIGINT NOT NULL REFERENCES components(id) ON DELETE RESTRICT,
    release_id BIGINT REFERENCES releases(id) ON DELETE RESTRICT,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_issues_issue_key UNIQUE (issue_key),
    CONSTRAINT ck_issues_issue_key_not_blank CHECK (LENGTH(TRIM(issue_key)) > 0),
    CONSTRAINT ck_issues_title_not_blank CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_issues_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_issues_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_issues_component_id ON issues (component_id);
CREATE INDEX idx_issues_release_id ON issues (release_id);
CREATE INDEX idx_issues_reporter_id ON issues (reporter_id);
CREATE INDEX idx_issues_assignee_id ON issues (assignee_id);
CREATE INDEX idx_issues_status ON issues (status);
CREATE INDEX idx_issues_component_status ON issues (component_id, status);
