package com.buglens.workspace.service;

import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.entity.WorkspaceRole;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAccessService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceAccessService(WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public WorkspaceMember requireMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(WorkspaceAccessDeniedException::new);
    }

    public WorkspaceMember requireMemberManager(Long workspaceId, Long userId) {
        WorkspaceMember membership = requireMember(workspaceId, userId);
        if (!membership.getRole().canManageMembers()) {
            throw new WorkspaceAccessDeniedException();
        }
        return membership;
    }

    public WorkspaceMember requireOwner(Long workspaceId, Long userId) {
        WorkspaceMember membership = requireMember(workspaceId, userId);
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new WorkspaceAccessDeniedException();
        }
        return membership;
    }
}
