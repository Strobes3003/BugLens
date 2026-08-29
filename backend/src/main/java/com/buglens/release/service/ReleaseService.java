package com.buglens.release.service;

import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.dto.request.CreateReleaseRequest;
import com.buglens.release.dto.request.UpdateReleaseRequest;
import com.buglens.release.dto.response.ReleaseResponse;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import com.buglens.release.exception.DuplicateReleaseVersionException;
import com.buglens.release.exception.InvalidReleaseFieldException;
import com.buglens.release.exception.ReleaseAccessDeniedException;
import com.buglens.release.exception.ReleaseNotFoundException;
import com.buglens.release.exception.ReleaseProjectNotFoundException;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public ReleaseService(
            ReleaseRepository releaseRepository,
            ProjectRepository projectRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public List<ReleaseResponse> listForProject(Long projectId, Long actorId) {
        Project project = requireProject(projectId);
        requireMember(project, actorId);
        return releaseRepository.findAllByProjectIdOrderByReleaseDateDescNameAsc(projectId)
                .stream()
                .map(ReleaseResponse::from)
                .toList();
    }

    @Transactional
    public ReleaseResponse create(CreateReleaseRequest request, Long actorId) {
        Project project = requireProject(request.projectId());
        requireManager(project, actorId);

        String name = normalize(request.name(), "name");
        String version = normalize(request.version(), "version");
        if (releaseRepository.existsByProjectIdAndVersionIgnoreCase(project.getId(), version)) {
            throw new DuplicateReleaseVersionException(version);
        }

        Release release = new Release(
                project,
                name,
                version,
                normalizeDescription(request.description()),
                ReleaseStatus.PLANNED,
                request.releaseDate()
        );
        return ReleaseResponse.from(releaseRepository.save(release));
    }

    public ReleaseResponse getById(Long releaseId, Long actorId) {
        Release release = getRelease(releaseId);
        requireMember(release.getProject(), actorId);
        return ReleaseResponse.from(release);
    }

    @Transactional
    public ReleaseResponse update(Long releaseId, UpdateReleaseRequest request, Long actorId) {
        Release release = getRelease(releaseId);
        requireManager(release.getProject(), actorId);

        String name = request.name() == null ? null : normalize(request.name(), "name");
        String version = request.version() == null ? null : normalize(request.version(), "version");
        if (version != null
                && !version.equalsIgnoreCase(release.getVersion())
                && releaseRepository.existsByProjectIdAndVersionIgnoreCase(release.getProject().getId(), version)) {
            throw new DuplicateReleaseVersionException(version);
        }

        release.updateDetails(
                name,
                version,
                normalizeDescription(request.description()),
                request.status(),
                request.releaseDate()
        );
        return ReleaseResponse.from(release);
    }

    private Release getRelease(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ReleaseNotFoundException(releaseId));
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ReleaseProjectNotFoundException(projectId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ReleaseAccessDeniedException();
        }
    }

    private void requireManager(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMemberManager(project.getWorkspace().getId(), actorId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ReleaseAccessDeniedException();
        }
    }

    private String normalize(String value, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidReleaseFieldException(field);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
