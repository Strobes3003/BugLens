package com.buglens.workspace.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.workspace.dto.request.UpdateWorkspaceRequest;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Workspace workspace;

    @Mock
    private User createdBy;

    @Test
    void updatesWorkspaceNameForManager() {
        WorkspaceService service = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                userRepository,
                workspaceAccessService
        );
        when(workspaceRepository.findById(3L)).thenReturn(Optional.of(workspace));
        when(workspace.getId()).thenReturn(3L);
        when(workspace.getCreatedBy()).thenReturn(createdBy);
        when(createdBy.getId()).thenReturn(11L);
        when(workspaceMemberRepository.findAllByWorkspaceIdOrderByJoinedAtAsc(3L))
                .thenReturn(List.<WorkspaceMember>of());

        service.update(3L, new UpdateWorkspaceRequest("  Platform Team  "), 11L);

        verify(workspaceAccessService).requireMemberManager(3L, 11L);
        verify(workspace).rename("Platform Team");
    }

    @Test
    void deletesWorkspaceOnlyAfterOwnerCheck() {
        WorkspaceService service = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                userRepository,
                workspaceAccessService
        );
        when(workspaceRepository.findById(3L)).thenReturn(Optional.of(workspace));

        service.delete(3L, 11L);

        verify(workspaceAccessService).requireOwner(3L, 11L);

        /*
         * Order matters: the members must leave the persistence context before the
         * workspace is removed, or Hibernate aborts the flush. Mocks cannot reproduce
         * that failure, so the ordering is pinned here instead.
         */
        InOrder order = inOrder(workspaceMemberRepository, workspaceRepository);
        order.verify(workspaceMemberRepository).deleteAllByWorkspaceId(3L);
        order.verify(workspaceRepository).delete(workspace);
    }

    @Test
    void rejectsBlankWorkspaceName() {
        WorkspaceService service = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                userRepository,
                workspaceAccessService
        );
        when(workspaceRepository.findById(3L)).thenReturn(Optional.of(workspace));

        assertThrows(
                RuntimeException.class,
                () -> service.update(3L, new UpdateWorkspaceRequest("  "), 11L)
        );
        verify(workspace, never()).rename("  ");
    }
}
