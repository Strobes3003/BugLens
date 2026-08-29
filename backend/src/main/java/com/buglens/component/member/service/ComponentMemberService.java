package com.buglens.component.member.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.Component;
import com.buglens.component.exception.ComponentAccessDeniedException;
import com.buglens.component.exception.ComponentMemberConflictException;
import com.buglens.component.exception.ComponentMemberNotFoundException;
import com.buglens.component.exception.ComponentMemberUserNotFoundException;
import com.buglens.component.exception.ComponentNotFoundException;
import com.buglens.component.member.dto.request.AddComponentMemberRequest;
import com.buglens.component.member.dto.request.UpdateComponentMemberRequest;
import com.buglens.component.member.dto.response.ComponentMemberResponse;
import com.buglens.component.member.entity.ComponentMember;
import com.buglens.component.member.entity.ComponentMemberRole;
import com.buglens.component.member.repository.ComponentMemberRepository;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ComponentMemberService {

    private final ComponentMemberRepository componentMemberRepository;
    private final ComponentRepository componentRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public ComponentMemberService(
            ComponentMemberRepository componentMemberRepository,
            ComponentRepository componentRepository,
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.componentMemberRepository = componentMemberRepository;
        this.componentRepository = componentRepository;
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public List<ComponentMemberResponse> list(Long componentId, Long actorId) {
        Component component = getComponent(componentId);
        requireWorkspaceMember(component, actorId);
        return componentMemberRepository.findAllByComponentIdOrderByAssignedAtAsc(componentId)
                .stream()
                .map(ComponentMemberResponse::from)
                .toList();
    }

    @Transactional
    public ComponentMemberResponse add(
            Long componentId,
            AddComponentMemberRequest request,
            Long actorId
    ) {
        Component component = getComponent(componentId);
        requireComponentManager(component, actorId);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(candidate -> workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                        component.getProject().getWorkspace().getId(), candidate.getId()
                ))
                .orElseThrow(() -> new ComponentMemberUserNotFoundException(email));

        if (componentMemberRepository.existsByComponentIdAndUserId(componentId, user.getId())) {
            throw new ComponentMemberConflictException(user.getId());
        }

        ComponentMember member = componentMemberRepository.save(
                new ComponentMember(component, user, request.role())
        );
        return ComponentMemberResponse.from(member);
    }

    @Transactional
    public ComponentMemberResponse update(
            Long componentId,
            Long userId,
            UpdateComponentMemberRequest request,
            Long actorId
    ) {
        Component component = getComponent(componentId);
        requireComponentManager(component, actorId);
        ComponentMember member = getMember(componentId, userId);
        member.changeRole(request.role());
        return ComponentMemberResponse.from(member);
    }

    @Transactional
    public void remove(Long componentId, Long userId, Long actorId) {
        Component component = getComponent(componentId);
        requireComponentManager(component, actorId);
        componentMemberRepository.delete(getMember(componentId, userId));
    }

    private Component getComponent(Long componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException(componentId));
    }

    private ComponentMember getMember(Long componentId, Long userId) {
        return componentMemberRepository.findByComponentIdAndUserId(componentId, userId)
                .orElseThrow(() -> new ComponentMemberNotFoundException(userId));
    }

    private void requireWorkspaceMember(Component component, Long actorId) {
        try {
            workspaceAccessService.requireMember(component.getProject().getWorkspace().getId(), actorId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ComponentAccessDeniedException();
        }
    }

    private void requireComponentManager(Component component, Long actorId) {
        try {
            workspaceAccessService.requireMemberManager(component.getProject().getWorkspace().getId(), actorId);
            return;
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException ignored) {
            // A component OWNER may manage the component's members.
        }

        ComponentMember membership = componentMemberRepository
                .findByComponentIdAndUserId(component.getId(), actorId)
                .orElseThrow(ComponentAccessDeniedException::new);
        if (membership.getRole() != ComponentMemberRole.OWNER) {
            throw new ComponentAccessDeniedException();
        }
    }
}
