-- Pre-calculated intelligence. Each table shares its primary key with the entity it scores, so
-- a row cannot describe a missing issue, component or release, and the cascade removes the
-- score along with what it measured.

CREATE TABLE issue_impacts (
    issue_id BIGINT PRIMARY KEY REFERENCES issues(id) ON DELETE CASCADE,
    impact_score INT NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_issue_impacts_score_range CHECK (impact_score BETWEEN 0 AND 100)
);

CREATE TABLE component_healths (
    component_id BIGINT PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
    health_score INT NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_component_healths_score_range CHECK (health_score BETWEEN 0 AND 100)
);

CREATE TABLE release_risks (
    release_id BIGINT PRIMARY KEY REFERENCES releases(id) ON DELETE CASCADE,
    risk_score INT NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_release_risks_score_range CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_issue_impacts_score ON issue_impacts (impact_score DESC);
