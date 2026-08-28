package com.buglens.project.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.project.dto.request.CreateProjectRequest;
import com.buglens.project.dto.request.UpdateProjectRequest;
import com.buglens.project.dto.response.ProjectResponse;
import com.buglens.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> list(
            @RequestParam(required = false) Long workspaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return projectService.listForUser(principal.getId(), workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return projectService.create(request, principal.getId());
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return projectService.getById(projectId, principal.getId());
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return projectService.update(projectId, request, principal.getId());
    }
}
