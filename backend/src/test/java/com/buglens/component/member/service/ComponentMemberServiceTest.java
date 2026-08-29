package com.buglens.component.member.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.exception.ComponentAccessDeniedException;
import com.buglens.component.exception.ComponentMemberConflictException;
import com.buglens.component.member.dto.request.AddComponentMemberRequest;
import com.buglens.component.member.dto.request.UpdateComponentMemberRequest;
import com.buglens.component.member.entity.ComponentMember;
import com.buglens.component.member.entity.ComponentMemberRole;
import com.buglens.component.member.repository.ComponentMemberRepository;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentMemberServiceTest {

    @Mock
    private ComponentMemberRepository componentMemberRepository;

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Component component;

    @Mock
    private Project project;

    @Mock
    private Workspace workspace;

    @Mock
    private User user;

    private ComponentMemberService componentMemberService;

    @BeforeEach
    void setUp() {
        componentMemberService = new ComponentMemberService(
                componentMemberRepository,
                componentRepository,
                userRepository,
                workspaceMemberRepository,
                workspaceAccessService
        );
        lenient().when(component.getId()).thenReturn(21L);
        lenient().when(component.getProject()).thenReturn(project);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(workspace.getId()).thenReturn(3L);
        lenient().when(user.getId()).thenReturn(11L);
        lenient().when(user.getName()).thenReturn("Karan");
        lenient().when(user.getEmail()).thenReturn("karan@example.com");
    }

    @Test
    void addsWorkspaceMemberToComponent() {
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));
        when(userRepository.findByEmailIgnoreCase("karan@example.com")).thenReturn(Optional.of(user));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(3L, 11L)).thenReturn(true);
        when(componentMemberRepository.existsByComponentIdAndUserId(21L, 11L)).thenReturn(false);
        when(componentMemberRepository.save(any(ComponentMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = componentMemberService.add(
                21L,
                new AddComponentMemberRequest(" Karan@Example.com ", ComponentMemberRole.DEVELOPER),
                7L
        );

        assertEquals(11L, response.userId());
        assertEquals(ComponentMemberRole.DEVELOPER, response.role());
        verify(workspaceAccessService).requireMemberManager(3L, 7L);
    }

    @Test
    void rejectsDuplicateComponentMember() {
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));
        when(userRepository.findByEmailIgnoreCase("karan@example.com")).thenReturn(Optional.of(user));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(3L, 11L)).thenReturn(true);
        when(componentMemberRepository.existsByComponentIdAndUserId(21L, 11L)).thenReturn(true);

        assertThrows(
                ComponentMemberConflictException.class,
                () -> componentMemberService.add(
                        21L,
                        new AddComponentMemberRequest("karan@example.com", ComponentMemberRole.QA),
                        7L
                )
        );
        verify(componentMemberRepository, never()).save(any(ComponentMember.class));
    }

    @Test
    void componentOwnerCanUpdateMemberRole() {
        ComponentMember owner = new ComponentMember(component, user, ComponentMemberRole.OWNER);
        ComponentMember target = new ComponentMember(component, user, ComponentMemberRole.QA);
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMemberManager(3L, 7L);
        when(componentMemberRepository.findByComponentIdAndUserId(21L, 7L)).thenReturn(Optional.of(owner));
        when(componentMemberRepository.findByComponentIdAndUserId(21L, 11L)).thenReturn(Optional.of(target));

        var response = componentMemberService.update(
                21L,
                11L,
                new UpdateComponentMemberRequest(ComponentMemberRole.DEVELOPER),
                7L
        );

        assertEquals(ComponentMemberRole.DEVELOPER, response.role());
    }

    @Test
    void rejectsNonOwnerComponentMemberFromManagingMembers() {
        ComponentMember developer = new ComponentMember(component, user, ComponentMemberRole.DEVELOPER);
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMemberManager(3L, 7L);
        when(componentMemberRepository.findByComponentIdAndUserId(21L, 7L)).thenReturn(Optional.of(developer));

        assertThrows(
                ComponentAccessDeniedException.class,
                () -> componentMemberService.update(
                        21L,
                        11L,
                        new UpdateComponentMemberRequest(ComponentMemberRole.QA),
                        7L
                )
        );
    }
}
