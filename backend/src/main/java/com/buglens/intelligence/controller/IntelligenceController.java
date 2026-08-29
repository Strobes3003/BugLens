package com.buglens.intelligence.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.intelligence.dto.response.ComponentHealthResponse;
import com.buglens.intelligence.dto.response.FixNextResponse;
import com.buglens.intelligence.dto.response.ReleaseRiskResponse;
import com.buglens.intelligence.service.IntelligenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IntelligenceController {

    private static final int DEFAULT_FIX_NEXT_LIMIT = 10;

    private final IntelligenceService intelligenceService;

    public IntelligenceController(IntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/projects/{projectId}/fix-next")
    public List<FixNextResponse> fixNext(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "" + DEFAULT_FIX_NEXT_LIMIT) int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return intelligenceService.getFixNext(projectId, limit, principal.getId())
                .stream()
                .map(FixNextResponse::from)
                .toList();
    }

    @GetMapping("/components/{componentId}/health")
    public ComponentHealthResponse componentHealth(
            @PathVariable Long componentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ComponentHealthResponse.from(
                intelligenceService.getComponentHealth(componentId, principal.getId())
        );
    }

    @GetMapping("/releases/{releaseId}/risk")
    public ReleaseRiskResponse releaseRisk(
            @PathVariable Long releaseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ReleaseRiskResponse.from(
                intelligenceService.getReleaseRisk(releaseId, principal.getId())
        );
    }
}
