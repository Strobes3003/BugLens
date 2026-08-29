package com.buglens.activity.repository;

import com.buglens.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<ActivityLog> findAllByIssueIdOrderByCreatedAtAsc(Long issueId);

    long countByIssueId(Long issueId);
}
