package com.buglens.comment.entity;

import com.buglens.auth.entity.User;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false, updatable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_edited", nullable = false)
    private boolean edited;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
    }

    public Comment(Issue issue, User author, String body) {
        this.issue = issue;
        this.author = author;
        this.body = body;
        this.edited = false;
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

    /**
     * Replaces the body and flags the comment as edited. {@code issue} and {@code author} are
     * mapped {@code updatable = false} and have no setters, so they are immutable after creation.
     */
    public void editBody(String newBody) {
        this.body = newBody;
        this.edited = true;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Issue getIssue() {
        return issue;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isEdited() {
        return edited;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
