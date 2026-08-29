package com.buglens.intelligence.entity;

import com.buglens.issue.entity.Issue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A stored impact score for one issue. The primary key is the issue's own id ({@link MapsId}),
 * so an issue can have at most one score and the row cannot outlive the issue.
 */
@Entity
@Table(name = "issue_impacts")
public class IssueImpact {

    @Id
    @Column(name = "issue_id")
    private Long issueId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @Column(name = "impact_score", nullable = false)
    private int impactScore;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected IssueImpact() {
    }

    public IssueImpact(Issue issue, int impactScore) {
        this.issue = issue;
        this.impactScore = impactScore;
        this.calculatedAt = Instant.now();
    }

    public void recalculatedAs(int impactScore) {
        this.impactScore = impactScore;
        this.calculatedAt = Instant.now();
    }

    public Long getIssueId() {
        return issueId;
    }

    public Issue getIssue() {
        return issue;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }
}
