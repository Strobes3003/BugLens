package com.buglens.dependency.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.dependency.dto.response.DependencyAnalysisResponse;
import com.buglens.dependency.service.DependencyAnalysisService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues/{issueId}/dependency-analysis")
public class DependencyAnalysisController {

    private final DependencyAnalysisService dependencyAnalysisService;

    public DependencyAnalysisController(DependencyAnalysisService dependencyAnalysisService) {
        this.dependencyAnalysisService = dependencyAnalysisService;
    }

    @GetMapping
    public DependencyAnalysisResponse analyze(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return dependencyAnalysisService.analyze(issueId, principal.getId());
    }
}
