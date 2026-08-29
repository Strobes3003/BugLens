package com.buglens.intelligence.dto.response;

import com.buglens.intelligence.entity.IssueImpact;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;

import java.time.Instant;

public record FixNextResponse(
        Long issueId,
        String issueKey,
        String title,
        IssueStatus status,
        IssuePriority priority,
        IssueSeverity severity,
        int impactScore,
        Instant createdAt,
        Instant calculatedAt
) {

    public static FixNextResponse from(IssueImpact impact) {
        Issue issue = impact.getIssue();
        return new FixNextResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getSeverity(),
                impact.getImpactScore(),
                issue.getCreatedAt(),
                impact.getCalculatedAt()
        );
    }
}
