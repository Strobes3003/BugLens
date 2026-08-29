package com.buglens.issue.entity;

import com.buglens.auth.entity.User;
import com.buglens.component.entity.Component;
import com.buglens.release.entity.Release;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_key", nullable = false, unique = true, updatable = false, length = 20)
    private String issueKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Issue() {
    }

    public Issue(
            String issueKey,
            String title,
            String description,
            IssueStatus status,
            IssuePriority priority,
            IssueSeverity severity,
            Component component,
            Release release,
            User reporter,
            User assignee
    ) {
        this.issueKey = issueKey;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.severity = severity;
        this.component = component;
        this.release = release;
        this.reporter = reporter;
        this.assignee = assignee;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(
            String title,
            String description,
            IssuePriority priority,
            IssueSeverity severity,
            Component component,
            Release release
    ) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (severity != null) {
            this.severity = severity;
        }
        if (component != null) {
            this.component = component;
        }
        if (release != null) {
            this.release = release;
        }
    }

    /**
     * The only status mutator on this entity. Transition legality is enforced by the workflow
     * engine before this is called; there is deliberately no public setter for {@code status},
     * so detail updates cannot change it.
     */
    public void transitionTo(IssueStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void moveToBacklog() {
        this.release = null;
    }

    public void assignTo(User assignee) {
        this.assignee = assignee;
    }

    public Long getId() {
        return id;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public Component getComponent() {
        return component;
    }

    public Release getRelease() {
        return release;
    }

    public User getReporter() {
        return reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
