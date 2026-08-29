package com.buglens.dependency.repository;

import com.buglens.dependency.entity.IssueDependency;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueDependencyRepository extends JpaRepository<IssueDependency, Long> {

    Optional<IssueDependency> findByBlockingIdAndBlockedId(Long blockingId, Long blockedId);

    boolean existsByBlockingIdAndBlockedId(Long blockingId, Long blockedId);

    /** Outgoing edges: the issues that {@code issueId} blocks. */
    @EntityGraph(attributePaths = {"blocked"})
    List<IssueDependency> findAllByBlockingIdOrderByCreatedAtAsc(Long issueId);

    /** Incoming edges: the issues that block {@code issueId}. */
    @EntityGraph(attributePaths = {"blocking"})
    List<IssueDependency> findAllByBlockedIdOrderByCreatedAtAsc(Long issueId);

    /** Direct downstream neighbours of {@code issueId}. One hop only. */
    @Query("select d.blocked.id from IssueDependency d where d.blocking.id = :issueId")
    List<Long> findBlockedIssueIds(@Param("issueId") Long issueId);

    /**
     * Walks the graph downstream from {@code startIssueId} and returns the path to
     * {@code targetIssueId}, or no row when the target is not reachable.
     *
     * <p>The whole traversal happens in the database. Walking it in Java meant one query per
     * visited node, which is O(V) round trips for what PostgreSQL can do in a single pass.
     *
     * <p>{@code NOT ... = ANY(path)} is a guard, not an optimisation: without it a cycle already
     * present in the data would make the recursion run forever. With it, no node is revisited on
     * a given path, so the query terminates on any input.
     *
     * <p>The path comes back as a comma-joined string rather than {@code bigint[]} so the result
     * has one unambiguous JDBC type regardless of array-mapping behaviour.
     */
    @Query(value = """
            WITH RECURSIVE reachable(node_id, path) AS (
                SELECT CAST(:startIssueId AS BIGINT), ARRAY[CAST(:startIssueId AS BIGINT)]
                UNION ALL
                SELECT d.blocked_issue_id, r.path || d.blocked_issue_id
                FROM issue_dependencies d
                JOIN reachable r ON d.blocking_issue_id = r.node_id
                WHERE NOT d.blocked_issue_id = ANY(r.path)
            )
            SELECT array_to_string(r.path, ',')
            FROM reachable r
            WHERE r.node_id = :targetIssueId
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findPathBetween(
            @Param("startIssueId") Long startIssueId,
            @Param("targetIssueId") Long targetIssueId
    );

    /**
     * Serialises dependency-graph writes for one project.
     *
     * <p>Cycle detection is read-then-write: two concurrent requests can each traverse an acyclic
     * graph, each conclude their edge is safe, and together close a loop. The unique constraint
     * cannot catch that — the two edges are different rows. A transaction-scoped advisory lock
     * makes the check and the insert atomic with respect to other writers in the same project,
     * and is released automatically on commit or rollback.
     *
     * <p>The key is the project id. The dependency graph is the only user of advisory locks in
     * this application; anything added later must choose a disjoint keyspace.
     */
    @Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock(:projectId)) AS acquired",
            nativeQuery = true)
    Boolean lockProjectGraph(@Param("projectId") Long projectId);
}
