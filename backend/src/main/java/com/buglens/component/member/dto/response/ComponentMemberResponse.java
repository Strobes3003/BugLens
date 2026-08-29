package com.buglens.component.member.dto.response;

import com.buglens.component.member.entity.ComponentMember;
import com.buglens.component.member.entity.ComponentMemberRole;

import java.time.Instant;

public record ComponentMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        ComponentMemberRole role,
        Instant assignedAt
) {

    public static ComponentMemberResponse from(ComponentMember member) {
        return new ComponentMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getAssignedAt()
        );
    }
}
