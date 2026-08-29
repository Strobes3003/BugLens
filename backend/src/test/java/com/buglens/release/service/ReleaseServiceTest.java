package com.buglens.release.service;

import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.dto.request.CreateReleaseRequest;
import com.buglens.release.dto.request.UpdateReleaseRequest;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import com.buglens.release.exception.DuplicateReleaseVersionException;
import com.buglens.release.exception.InvalidReleaseFieldException;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Project project;

    @Mock
    private com.buglens.workspace.entity.Workspace workspace;

    private ReleaseService releaseService;

    @BeforeEach
    void setUp() {
        releaseService = new ReleaseService(releaseRepository, projectRepository, workspaceAccessService);
        lenient().when(project.getId()).thenReturn(7L);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(workspace.getId()).thenReturn(3L);
        lenient().when(project.getName()).thenReturn("BugLens");
        lenient().when(project.getKey()).thenReturn("BL");
        lenient().when(project.getStatus()).thenReturn(ProjectStatus.ACTIVE);
    }

    @Test
    void createsTrimmedPlannedReleaseForWorkspaceManager() {
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(releaseRepository.existsByProjectIdAndVersionIgnoreCase(7L, "1.0.0"))
                .thenReturn(false);
        when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = releaseService.create(
                new CreateReleaseRequest(7L, "  Initial release  ", "  1.0.0  ", "  First version  ", LocalDate.of(2026, 8, 29)),
                11L
        );

        assertEquals("Initial release", response.name());
        assertEquals("1.0.0", response.version());
        assertEquals("First version", response.description());
        assertEquals(ReleaseStatus.PLANNED, response.status());
        assertEquals(LocalDate.of(2026, 8, 29), response.releaseDate());
        verify(workspaceAccessService).requireMemberManager(3L, 11L);
    }

    @Test
    void rejectsDuplicateVersionIgnoringCase() {
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(releaseRepository.existsByProjectIdAndVersionIgnoreCase(7L, "v1.0"))
                .thenReturn(true);

        assertThrows(
                DuplicateReleaseVersionException.class,
                () -> releaseService.create(new CreateReleaseRequest(7L, "Release 1", "v1.0", null, null), 11L)
        );
        verify(releaseRepository, never()).save(any(Release.class));
    }

    @Test
    void rejectsBlankVersionDuringUpdate() {
        Release release = new Release(project, "Release 1", "1.0.0", null, ReleaseStatus.PLANNED, null);
        when(releaseRepository.findById(21L)).thenReturn(Optional.of(release));

        assertThrows(
                InvalidReleaseFieldException.class,
                () -> releaseService.update(21L, new UpdateReleaseRequest(null, "  ", null, null, null), 11L)
        );
    }

    @Test
    void updatesReleaseStatusAndDate() {
        Release release = new Release(project, "Release 1", "1.0.0", null, ReleaseStatus.PLANNED, null);
        when(releaseRepository.findById(21L)).thenReturn(Optional.of(release));

        var response = releaseService.update(
                21L,
                new UpdateReleaseRequest(null, null, " published ", ReleaseStatus.RELEASED, LocalDate.of(2026, 9, 1)),
                11L
        );

        assertEquals("Release 1", response.name());
        assertEquals("published", response.description());
        assertEquals(ReleaseStatus.RELEASED, response.status());
        assertEquals(LocalDate.of(2026, 9, 1), response.releaseDate());
        verify(workspaceAccessService).requireMemberManager(3L, 11L);
    }
}
