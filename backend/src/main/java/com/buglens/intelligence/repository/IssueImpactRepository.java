package com.buglens.intelligence.repository;

import com.buglens.intelligence.entity.IssueImpact;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssueImpactRepository extends JpaRepository<IssueImpact, Long> {

    /**
     * Fix Next: the highest-impact issues in a project, oldest first among equals so a tie does
     * not reshuffle between calls and the issue that has waited longest wins.
     *
     * <p>Reads stored scores only — no graph traversal happens on this path.
     */
    @EntityGraph(attributePaths = {"issue"})
    @Query("""
            select im from IssueImpact im
            join im.issue i
            where i.component.project.id = :projectId
            order by im.impactScore desc, i.createdAt asc
            """)
    List<IssueImpact> findFixNext(@Param("projectId") Long projectId, Pageable pageable);
}
