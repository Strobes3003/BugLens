package com.buglens.workspace.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.workspace.dto.request.AddWorkspaceMemberRequest;
import com.buglens.workspace.dto.request.CreateWorkspaceRequest;
import com.buglens.workspace.dto.request.UpdateWorkspaceMemberRequest;
import com.buglens.workspace.dto.response.WorkspaceMemberResponse;
import com.buglens.workspace.dto.response.WorkspaceResponse;
import com.buglens.workspace.dto.response.WorkspaceSummaryResponse;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.entity.WorkspaceRole;
import com.buglens.workspace.exception.InvalidWorkspaceRoleException;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.exception.WorkspaceConflictException;
import com.buglens.workspace.exception.WorkspaceMemberNotFoundException;
import com.buglens.workspace.exception.WorkspaceNotFoundException;
import com.buglens.workspace.exception.WorkspaceUserNotFoundException;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public List<WorkspaceSummaryResponse> listForUser(Long userId) {
        return workspaceMemberRepository.findAllByUserIdOrderByJoinedAtAsc(userId).stream()
                .map(WorkspaceSummaryResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new WorkspaceUserNotFoundException(String.valueOf(creatorId)));
        String name = request.name().trim();
        Workspace workspace = workspaceRepository.save(
                new Workspace(name, createUniqueSlug(name), creator)
        );
        workspaceMemberRepository.save(new WorkspaceMember(workspace, creator, WorkspaceRole.OWNER));
        return toResponse(workspace);
    }

    public WorkspaceResponse getById(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspace(workspaceId);
        workspaceAccessService.requireMember(workspaceId, userId);
        return toResponse(workspace);
    }

    @Transactional
    public WorkspaceMemberResponse addMember(
            Long workspaceId,
            AddWorkspaceMemberRequest request,
            Long actorId
    ) {
        getWorkspace(workspaceId);
        WorkspaceMember actor = workspaceAccessService.requireMemberManager(workspaceId, actorId);
        validateAssignableRole(request.role());

        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new WorkspaceUserNotFoundException(email));
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new WorkspaceConflictException("User is already a member of this workspace");
        }
        if (actor.getRole() == WorkspaceRole.ADMIN && request.role() == WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException();
        }

        WorkspaceMember saved = workspaceMemberRepository.save(
                new WorkspaceMember(getWorkspace(workspaceId), user, request.role())
        );
        return WorkspaceMemberResponse.from(saved);
    }

    @Transactional
    public WorkspaceMemberResponse updateMember(
            Long workspaceId,
            Long userId,
            UpdateWorkspaceMemberRequest request,
            Long actorId
    ) {
        getWorkspace(workspaceId);
        WorkspaceMember actor = workspaceAccessService.requireMemberManager(workspaceId, actorId);
        WorkspaceMember target = getMember(workspaceId, userId);
        validateAssignableRole(request.role());
        validateAdminTarget(actor, target);
        target.changeRole(request.role());
        return WorkspaceMemberResponse.from(target);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long userId, Long actorId) {
        getWorkspace(workspaceId);
        WorkspaceMember actor = workspaceAccessService.requireMemberManager(workspaceId, actorId);
        WorkspaceMember target = getMember(workspaceId, userId);
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceRoleException("The workspace owner cannot be removed");
        }
        validateAdminTarget(actor, target);
        workspaceMemberRepository.delete(target);
    }

    private Workspace getWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
    }

    private WorkspaceMember getMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(userId));
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        List<WorkspaceMemberResponse> members = workspaceMemberRepository
                .findAllByWorkspaceIdOrderByJoinedAtAsc(workspace.getId())
                .stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
        return WorkspaceResponse.from(workspace, members);
    }

    private String createUniqueSlug(String name) {
        String baseSlug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (baseSlug.isBlank()) {
            throw new WorkspaceConflictException("Workspace name must contain letters or numbers");
        }

        String candidate = baseSlug;
        int suffix = 2;
        while (workspaceRepository.existsBySlugIgnoreCase(candidate)) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private void validateAssignableRole(WorkspaceRole role) {
        if (role == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceRoleException("OWNER cannot be assigned through member management");
        }
    }

    private void validateAdminTarget(WorkspaceMember actor, WorkspaceMember target) {
        if (actor.getRole() == WorkspaceRole.ADMIN && target.getRole() == WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
