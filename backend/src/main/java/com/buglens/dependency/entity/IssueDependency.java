package com.buglens.dependency.entity;

import com.buglens.issue.entity.Issue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A directed edge in the dependency graph: {@code blocking} must be resolved before
 * {@code blocked} can proceed. Edges are immutable — to change a dependency, remove it and add
 * the one you want, so the graph never passes through a half-rewritten state.
 */
@Entity
@Table(
        name = "issue_dependencies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_issue_dependencies_edge",
                columnNames = {"blocking_issue_id", "blocked_issue_id"}
        )
)
public class IssueDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocking_issue_id", nullable = false, updatable = false)
    private Issue blocking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_issue_id", nullable = false, updatable = false)
    private Issue blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IssueDependency() {
    }

    public IssueDependency(Issue blocking, Issue blocked) {
        this.blocking = blocking;
        this.blocked = blocked;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Issue getBlocking() {
        return blocking;
    }

    public Issue getBlocked() {
        return blocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
