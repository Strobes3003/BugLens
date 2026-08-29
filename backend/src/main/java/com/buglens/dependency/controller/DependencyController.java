package com.buglens.dependency.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.dependency.dto.request.CreateDependencyRequest;
import com.buglens.dependency.dto.response.DependencyResponse;
import com.buglens.dependency.dto.response.IssueDependenciesResponse;
import com.buglens.dependency.service.DependencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The path issue is always the blocking side of the edge: posting to
 * {@code /api/issues/1/dependencies} with {@code blockedIssueId: 2} records "1 blocks 2".
 */
@RestController
@RequestMapping("/api/issues/{issueId}/dependencies")
public class DependencyController {

    private final DependencyService dependencyService;

    public DependencyController(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DependencyResponse create(
            @PathVariable Long issueId,
            @Valid @RequestBody CreateDependencyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return dependencyService.addDependency(issueId, request.blockedIssueId(), principal.getId());
    }

    @GetMapping
    public IssueDependenciesResponse list(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return dependencyService.getDependencies(issueId, principal.getId());
    }

    @DeleteMapping("/{blockedIssueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long issueId,
            @PathVariable Long blockedIssueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dependencyService.removeDependency(issueId, blockedIssueId, principal.getId());
    }
}
