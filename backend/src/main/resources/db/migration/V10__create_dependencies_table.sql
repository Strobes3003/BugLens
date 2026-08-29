CREATE TABLE issue_dependencies (
    id BIGSERIAL PRIMARY KEY,
    blocking_issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    blocked_issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_issue_dependencies_edge UNIQUE (blocking_issue_id, blocked_issue_id),
    CONSTRAINT ck_issue_dependencies_no_self_edge CHECK (blocking_issue_id <> blocked_issue_id)
);

CREATE INDEX idx_issue_dependencies_blocking ON issue_dependencies (blocking_issue_id);
CREATE INDEX idx_issue_dependencies_blocked ON issue_dependencies (blocked_issue_id);
