package com.buglens.dependency.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.dependency.dto.response.DependencyResponse;
import com.buglens.dependency.service.DependencyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Whole-graph read for a project, so the dependency view can render every edge in one request
 * rather than walking issue by issue.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/dependencies")
public class ProjectDependencyController {

    private final DependencyService dependencyService;

    public ProjectDependencyController(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @GetMapping
    public List<DependencyResponse> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return dependencyService.listForProject(projectId, principal.getId());
    }
}
