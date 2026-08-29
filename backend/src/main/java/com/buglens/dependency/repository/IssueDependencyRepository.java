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
     * Every issue reachable downstream of {@code issueId} — everything it blocks, directly or
     * through any chain.
     *
     * <p>Recursion uses {@code UNION}, not {@code UNION ALL}. That single word is what makes the
     * result a set: a node reachable by several routes through a diamond is produced once, so
     * the caller counts distinct affected issues rather than distinct paths. It is also the
     * termination condition — once no new node ids appear the recursion stops, so the query
     * completes even on a graph that already contains a cycle.
     *
     * <p>The start node is excluded: an issue is not in its own blast radius.
     */
    @Query(value = """
            WITH RECURSIVE downstream(node_id) AS (
                SELECT d.blocked_issue_id
                FROM issue_dependencies d
                WHERE d.blocking_issue_id = :issueId
                UNION
                SELECT d.blocked_issue_id
                FROM issue_dependencies d
                JOIN downstream ds ON d.blocking_issue_id = ds.node_id
            )
            SELECT ds.node_id FROM downstream ds WHERE ds.node_id <> :issueId
            """, nativeQuery = true)
    List<Long> findDeepDownstreamBlockedIds(@Param("issueId") Long issueId);

    /**
     * Every issue upstream of {@code issueId} — everything that blocks it, directly or through
     * any chain. The mirror of {@link #findDeepDownstreamBlockedIds}, walking edges in reverse.
     */
    @Query(value = """
            WITH RECURSIVE upstream(node_id) AS (
                SELECT d.blocking_issue_id
                FROM issue_dependencies d
                WHERE d.blocked_issue_id = :issueId
                UNION
                SELECT d.blocking_issue_id
                FROM issue_dependencies d
                JOIN upstream us ON d.blocked_issue_id = us.node_id
            )
            SELECT us.node_id FROM upstream us WHERE us.node_id <> :issueId
            """, nativeQuery = true)
    List<Long> findDeepUpstreamBlockerIds(@Param("issueId") Long issueId);

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
