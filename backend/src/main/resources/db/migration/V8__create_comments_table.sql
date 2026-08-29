CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body TEXT NOT NULL,
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_comments_body_not_blank CHECK (LENGTH(TRIM(body)) > 0)
);

CREATE INDEX idx_comments_issue_id ON comments (issue_id);
CREATE INDEX idx_comments_created_at ON comments (created_at);
CREATE INDEX idx_comments_issue_created_at ON comments (issue_id, created_at);
CREATE INDEX idx_comments_author_id ON comments (author_id);
