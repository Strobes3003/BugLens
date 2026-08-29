CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action_type VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_activity_logs_description_not_blank CHECK (LENGTH(TRIM(description)) > 0),
    CONSTRAINT ck_activity_logs_action_type CHECK (
        action_type IN ('ISSUE_CREATED', 'ISSUE_UPDATED', 'STATUS_CHANGED', 'COMMENT_ADDED')
    )
);

CREATE INDEX idx_activity_logs_issue_id ON activity_logs (issue_id);
CREATE INDEX idx_activity_logs_issue_created_at ON activity_logs (issue_id, created_at);
CREATE INDEX idx_activity_logs_actor_id ON activity_logs (actor_id);
