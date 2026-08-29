package com.buglens.issue.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * Allocates per-project issue numbers.
 *
 * <p>The allocation is a single atomic upsert: PostgreSQL takes a row lock on the project's
 * counter row for the duration of the statement, so concurrent creates in the same project are
 * serialised by the database and each caller receives a distinct number. Numbers are never
 * reused, even when an issue is deleted, so an issue key remains a stable external reference.
 */
@Repository
public class IssueSequenceRepository {

    private static final String ALLOCATE_NEXT = """
            INSERT INTO project_issue_sequences (project_id, last_value)
            VALUES (:projectId, 1)
            ON CONFLICT (project_id)
            DO UPDATE SET last_value = project_issue_sequences.last_value + 1
            RETURNING last_value
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public long allocateNext(Long projectId) {
        Object allocated = entityManager.createNativeQuery(ALLOCATE_NEXT)
                .setParameter("projectId", projectId)
                .getSingleResult();
        return ((Number) allocated).longValue();
    }
}
