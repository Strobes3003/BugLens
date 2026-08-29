CREATE TABLE project_issue_sequences (
    project_id BIGINT PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    last_value BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_issue_sequences_last_value CHECK (last_value >= 0)
);
