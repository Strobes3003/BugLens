package com.buglens.activity.dto.response;

import com.buglens.activity.entity.ActivityAction;
import com.buglens.activity.entity.ActivityLog;
import com.buglens.auth.entity.User;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        Long issueId,
        ActivityActor actor,
        ActivityAction actionType,
        String description,
        Instant createdAt
) {

    public record ActivityActor(Long id, String name) {

        public static ActivityActor from(User user) {
            return new ActivityActor(user.getId(), user.getName());
        }
    }

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getIssue().getId(),
                ActivityActor.from(log.getActor()),
                log.getActionType(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}
