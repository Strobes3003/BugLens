CREATE TABLE component_members (
    id BIGSERIAL PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_component_members_component_user UNIQUE (component_id, user_id),
    CONSTRAINT ck_component_members_role CHECK (role IN ('OWNER', 'DEVELOPER', 'QA', 'WATCHER'))
);

CREATE INDEX idx_component_members_component_id ON component_members (component_id);
CREATE INDEX idx_component_members_user_id ON component_members (user_id);
