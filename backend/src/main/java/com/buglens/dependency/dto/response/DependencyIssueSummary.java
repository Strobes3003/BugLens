package com.buglens.dependency.dto.response;

import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;

public record DependencyIssueSummary(
        Long id,
        String issueKey,
        String title,
        IssueStatus status,
        IssuePriority priority,
        IssueSeverity severity
) {

    public static DependencyIssueSummary from(Issue issue) {
        return new DependencyIssueSummary(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getSeverity()
        );
    }
}
