-- Dependency changes will be recorded as activity in Phase 9. Widen the allowed action types
-- now so the constraint is not the thing that blocks that work.
ALTER TABLE activity_logs DROP CONSTRAINT IF EXISTS ck_activity_logs_action_type;

ALTER TABLE activity_logs
    ADD CONSTRAINT ck_activity_logs_action_type CHECK (
        action_type IN (
            'ISSUE_CREATED',
            'ISSUE_UPDATED',
            'STATUS_CHANGED',
            'COMMENT_ADDED',
            'DEPENDENCY_ADDED',
            'DEPENDENCY_REMOVED'
        )
    );
