package com.buglens.dependency.dto.response;

import com.buglens.dependency.entity.IssueDependency;

import java.time.Instant;

/** A single directed edge. */
public record DependencyResponse(
        Long id,
        DependencyIssueSummary blockingIssue,
        DependencyIssueSummary blockedIssue,
        Instant createdAt
) {

    public static DependencyResponse from(IssueDependency dependency) {
        return new DependencyResponse(
                dependency.getId(),
                DependencyIssueSummary.from(dependency.getBlocking()),
                DependencyIssueSummary.from(dependency.getBlocked()),
                dependency.getCreatedAt()
        );
    }
}
