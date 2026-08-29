package com.buglens.issue.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.service.IssueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Project-scoped issue listing. Kept separate from {@link IssueController} because the path is
 * nested under {@code /api/projects}, which cannot be expressed alongside the {@code /api/issues}
 * class-level mapping.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/issues")
public class ProjectIssueController {

    private final IssueService issueService;

    public ProjectIssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    public List<IssueResponse> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long componentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (componentId != null) {
            return issueService.listForComponent(projectId, componentId, principal.getId());
        }
        return issueService.listForProject(projectId, principal.getId());
    }
}
