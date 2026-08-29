package com.buglens.component.service;

import com.buglens.component.dto.request.CreateComponentRequest;
import com.buglens.component.dto.request.UpdateComponentRequest;
import com.buglens.component.dto.response.ComponentResponse;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.exception.ComponentAccessDeniedException;
import com.buglens.component.exception.ComponentNotFoundException;
import com.buglens.component.exception.ComponentProjectNotFoundException;
import com.buglens.component.exception.DuplicateComponentNameException;
import com.buglens.component.exception.InvalidComponentNameException;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public ComponentService(
            ComponentRepository componentRepository,
            ProjectRepository projectRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.componentRepository = componentRepository;
        this.projectRepository = projectRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public List<ComponentResponse> listForProject(Long projectId, Long actorId) {
        Project project = requireProject(projectId);
        requireMember(project, actorId);
        return componentRepository.findAllByProjectIdOrderByNameAsc(projectId)
                .stream()
                .map(ComponentResponse::from)
                .toList();
    }

    @Transactional
    public ComponentResponse create(CreateComponentRequest request, Long actorId) {
        Project project = requireProject(request.projectId());
        requireManager(project, actorId);

        String name = normalizeName(request.name());
        if (componentRepository.existsByProjectIdAndNameIgnoreCase(project.getId(), name)) {
            throw new DuplicateComponentNameException(name);
        }

        Component component = new Component(
                project,
                name,
                normalizeDescription(request.description()),
                ComponentStatus.ACTIVE
        );
        return ComponentResponse.from(componentRepository.save(component));
    }

    public ComponentResponse getById(Long componentId, Long actorId) {
        Component component = getComponent(componentId);
        requireMember(component.getProject(), actorId);
        return ComponentResponse.from(component);
    }

    @Transactional
    public ComponentResponse update(Long componentId, UpdateComponentRequest request, Long actorId) {
        Component component = getComponent(componentId);
        requireManager(component.getProject(), actorId);

        String name = request.name();
        if (name != null) {
            name = normalizeName(name);
            if (!name.equalsIgnoreCase(component.getName())
                    && componentRepository.existsByProjectIdAndNameIgnoreCase(component.getProject().getId(), name)) {
                throw new DuplicateComponentNameException(name);
            }
        }

        component.updateDetails(name, normalizeDescription(request.description()), request.status());
        return ComponentResponse.from(component);
    }

    @Transactional
    public void delete(Long componentId, Long actorId) {
        Component component = getComponent(componentId);
        requireManager(component.getProject(), actorId);
        componentRepository.delete(component);
    }

    private Component getComponent(Long componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException(componentId));
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ComponentProjectNotFoundException(projectId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ComponentAccessDeniedException();
        }
    }

    private void requireManager(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMemberManager(project.getWorkspace().getId(), actorId);
        } catch (com.buglens.workspace.exception.WorkspaceAccessDeniedException exception) {
            throw new ComponentAccessDeniedException();
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? null : name.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidComponentNameException();
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
