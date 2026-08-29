package com.buglens.activity.controller;

import com.buglens.activity.dto.response.ActivityLogResponse;
import com.buglens.activity.service.ActivityLogService;
import com.buglens.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/issues/{issueId}/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public List<ActivityLogResponse> list(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return activityLogService.listForIssue(issueId, principal.getId());
    }
}
