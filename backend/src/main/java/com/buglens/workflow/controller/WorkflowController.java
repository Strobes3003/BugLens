package com.buglens.workflow.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.workflow.dto.request.TransitionIssueRequest;
import com.buglens.workflow.dto.response.AllowedTransitionsResponse;
import com.buglens.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues/{issueId}/transitions")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public IssueResponse transition(
            @PathVariable Long issueId,
            @Valid @RequestBody TransitionIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workflowService.transition(issueId, request, principal.getId());
    }

    @GetMapping
    public AllowedTransitionsResponse allowedTransitions(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workflowService.getAllowedTransitions(issueId, principal.getId());
    }
}
