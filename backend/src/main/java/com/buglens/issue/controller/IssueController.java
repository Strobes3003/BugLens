package com.buglens.issue.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.issue.dto.request.CreateIssueRequest;
import com.buglens.issue.dto.request.UpdateIssueRequest;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return issueService.create(request, principal.getId());
    }

    @GetMapping("/{issueId}")
    public IssueResponse getById(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return issueService.getById(issueId, principal.getId());
    }

    @GetMapping("/key/{issueKey}")
    public IssueResponse getByKey(
            @PathVariable String issueKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return issueService.getByKey(issueKey, principal.getId());
    }

    @PatchMapping("/{issueId}")
    public IssueResponse update(
            @PathVariable Long issueId,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return issueService.updateDetails(issueId, request, principal.getId());
    }

    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        issueService.delete(issueId, principal.getId());
    }
}
