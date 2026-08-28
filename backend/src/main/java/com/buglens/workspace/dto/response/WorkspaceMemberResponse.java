package com.buglens.workspace.dto.response;

import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.entity.WorkspaceRole;

import java.time.Instant;

public record WorkspaceMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        WorkspaceRole role,
        Instant joinedAt
) {

    public static WorkspaceMemberResponse from(WorkspaceMember membership) {
        return new WorkspaceMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
