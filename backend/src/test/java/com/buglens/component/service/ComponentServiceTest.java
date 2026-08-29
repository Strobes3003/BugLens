package com.buglens.component.service;

import com.buglens.component.dto.request.CreateComponentRequest;
import com.buglens.component.dto.request.UpdateComponentRequest;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.exception.DuplicateComponentNameException;
import com.buglens.component.exception.InvalidComponentNameException;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceRole;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Project project;

    @Mock
    private Workspace workspace;

    private ComponentService componentService;

    @BeforeEach
    void setUp() {
        componentService = new ComponentService(componentRepository, projectRepository, workspaceAccessService);
        lenient().when(project.getId()).thenReturn(7L);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(workspace.getId()).thenReturn(3L);
        lenient().when(project.getName()).thenReturn("BugLens");
        lenient().when(project.getKey()).thenReturn("BL");
        lenient().when(project.getStatus()).thenReturn(ProjectStatus.ACTIVE);
    }

    @Test
    void createsTrimmedActiveComponentForWorkspaceManager() {
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(componentRepository.existsByProjectIdAndNameIgnoreCase(7L, "API"))
                .thenReturn(false);
        when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = componentService.create(
                new CreateComponentRequest(7L, "  API  ", "  HTTP endpoints  "),
                11L
        );

        assertEquals("API", response.name());
        assertEquals("HTTP endpoints", response.description());
        assertEquals(ComponentStatus.ACTIVE, response.status());
        verify(workspaceAccessService).requireMemberManager(3L, 11L);
    }

    @Test
    void rejectsDuplicateComponentNameIgnoringCase() {
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(componentRepository.existsByProjectIdAndNameIgnoreCase(7L, "api"))
                .thenReturn(true);

        assertThrows(
                DuplicateComponentNameException.class,
                () -> componentService.create(new CreateComponentRequest(7L, "api", null), 11L)
        );
        verify(componentRepository, never()).save(any(Component.class));
    }

    @Test
    void rejectsBlankNameDuringUpdate() {
        Component component = new Component(project, "API", null, ComponentStatus.ACTIVE);
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));

        assertThrows(
                InvalidComponentNameException.class,
                () -> componentService.update(21L, new UpdateComponentRequest("  ", null, null), 11L)
        );
    }

    @Test
    void updatesStatusAndDescription() {
        Component component = new Component(project, "API", null, ComponentStatus.ACTIVE);
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));

        var response = componentService.update(
                21L,
                new UpdateComponentRequest(null, " retired ", ComponentStatus.ARCHIVED),
                11L
        );

        assertEquals("API", response.name());
        assertEquals("retired", response.description());
        assertEquals(ComponentStatus.ARCHIVED, response.status());
        verify(workspaceAccessService).requireMemberManager(3L, 11L);
    }

    @Test
    void deletesComponentForWorkspaceManager() {
        Component component = new Component(project, "API", null, ComponentStatus.ACTIVE);
        when(componentRepository.findById(21L)).thenReturn(Optional.of(component));

        componentService.delete(21L, 11L);

        verify(workspaceAccessService).requireMemberManager(3L, 11L);
        verify(componentRepository).delete(component);
    }
}
