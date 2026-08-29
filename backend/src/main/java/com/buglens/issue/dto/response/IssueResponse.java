package com.buglens.issue.dto.response;

import com.buglens.auth.entity.User;
import com.buglens.component.entity.Component;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.project.entity.Project;
import com.buglens.release.entity.Release;

import java.time.Instant;

public record IssueResponse(
        Long id,
        String issueKey,
        String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        IssueSeverity severity,
        Long projectId,
        String projectKey,
        String projectName,
        Long componentId,
        String componentName,
        Long releaseId,
        String releaseName,
        Long reporterId,
        String reporterName,
        Long assigneeId,
        String assigneeName,
        Instant createdAt,
        Instant updatedAt
) {

    public static IssueResponse from(Issue issue) {
        Component component = issue.getComponent();
        Project project = component.getProject();
        Release release = issue.getRelease();
        User reporter = issue.getReporter();
        User assignee = issue.getAssignee();

        return new IssueResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getSeverity(),
                project.getId(),
                project.getKey(),
                project.getName(),
                component.getId(),
                component.getName(),
                release == null ? null : release.getId(),
                release == null ? null : release.getName(),
                reporter.getId(),
                reporter.getName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}
