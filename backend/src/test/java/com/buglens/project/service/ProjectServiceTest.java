package com.buglens.project.service;

import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.repository.WorkspaceRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private ProjectKeyGenerator projectKeyGenerator;

    @Mock
    private Project project;

    @Mock
    private Workspace workspace;

    @Test
    void deletesProjectForWorkspaceManager() {
        ProjectService service = new ProjectService(
                projectRepository,
                workspaceRepository,
                workspaceMemberRepository,
                workspaceAccessService,
                projectKeyGenerator
        );
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(project.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(3L);

        service.delete(7L, 11L);

        verify(workspaceAccessService).requireMemberManager(3L, 11L);
        verify(projectRepository).delete(project);
    }
}
