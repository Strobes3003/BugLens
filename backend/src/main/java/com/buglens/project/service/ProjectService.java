package com.buglens.project.service;

import com.buglens.project.dto.request.CreateProjectRequest;
import com.buglens.project.dto.request.UpdateProjectRequest;
import com.buglens.project.dto.response.ProjectResponse;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.exception.DuplicateProjectKeyException;
import com.buglens.project.exception.InvalidProjectNameException;
import com.buglens.project.exception.ProjectAccessDeniedException;
import com.buglens.project.exception.ProjectNotFoundException;
import com.buglens.project.exception.ProjectWorkspaceNotFoundException;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.repository.WorkspaceRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private static final int MAX_KEY_LENGTH = 10;

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final ProjectKeyGenerator projectKeyGenerator;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceAccessService workspaceAccessService,
            ProjectKeyGenerator projectKeyGenerator
    ) {
        this.projectRepository = projectRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.projectKeyGenerator = projectKeyGenerator;
    }

    public List<ProjectResponse> listForUser(Long userId, Long workspaceId) {
        Collection<Project> projects;
        if (workspaceId != null) {
            requireWorkspace(workspaceId);
            requireMember(workspaceId, userId);
            projects = projectRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId);
        } else {
            List<Long> workspaceIds = workspaceMemberRepository.findAllByUserIdOrderByJoinedAtAsc(userId)
                    .stream()
                    .map(member -> member.getWorkspace().getId())
                    .toList();
            if (workspaceIds.isEmpty()) {
                return List.of();
            }
            projects = projectRepository.findAllByWorkspaceIdInOrderByNameAsc(workspaceIds);
        }
        return projects.stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long actorId) {
        Workspace workspace = requireWorkspace(request.workspaceId());
        requireManager(workspace.getId(), actorId);

        String name = request.name().trim();
        String projectKey = resolveProjectKey(request.key(), name, workspace.getId());
        Project project = new Project(
                workspace,
                name,
                projectKey,
                normalizeDescription(request.description()),
                ProjectStatus.ACTIVE
        );
        return ProjectResponse.from(projectRepository.save(project));
    }

    public ProjectResponse getById(Long projectId, Long actorId) {
        Project project = getProject(projectId);
        requireMember(project.getWorkspace().getId(), actorId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request, Long actorId) {
        Project project = getProject(projectId);
        requireManager(project.getWorkspace().getId(), actorId);

        String name = request.name();
        if (name != null) {
            name = name.trim();
            if (name.isBlank()) {
                throw new InvalidProjectNameException();
            }
        }
        project.updateDetails(name, normalizeDescription(request.description()), request.status());
        return ProjectResponse.from(project);
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Workspace requireWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ProjectWorkspaceNotFoundException(workspaceId));
    }

    private void requireMember(Long workspaceId, Long userId) {
        try {
            workspaceAccessService.requireMember(workspaceId, userId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ProjectAccessDeniedException();
        }
    }

    private void requireManager(Long workspaceId, Long userId) {
        try {
            workspaceAccessService.requireMemberManager(workspaceId, userId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ProjectAccessDeniedException();
        }
    }

    private String resolveProjectKey(String requestedKey, String projectName, Long workspaceId) {
        if (requestedKey != null && !requestedKey.isBlank()) {
            String normalizedKey = projectKeyGenerator.normalizeProvidedKey(requestedKey);
            if (projectRepository.existsByWorkspaceIdAndKeyIgnoreCase(workspaceId, normalizedKey)) {
                throw new DuplicateProjectKeyException(normalizedKey);
            }
            return normalizedKey;
        }

        String baseKey = projectKeyGenerator.generateBaseKey(projectName);
        String candidate = baseKey;
        int suffix = 2;
        while (projectRepository.existsByWorkspaceIdAndKeyIgnoreCase(workspaceId, candidate)) {
            String suffixText = String.valueOf(suffix++);
            int baseLength = Math.min(baseKey.length(), MAX_KEY_LENGTH - suffixText.length());
            candidate = baseKey.substring(0, baseLength) + suffixText;
        }
        return candidate;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        return description.isBlank() ? null : description.trim();
    }
}
