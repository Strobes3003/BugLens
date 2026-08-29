package com.buglens.issue.repository;

import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByIssueKey(String issueKey);

    List<Issue> findAllByComponentIdOrderByCreatedAtDesc(Long componentId);

    List<Issue> findAllByReleaseIdOrderByCreatedAtDesc(Long releaseId);

    List<Issue> findAllByComponentProjectIdOrderByCreatedAtDesc(Long projectId);

    long countByComponentIdAndStatus(Long componentId, IssueStatus status);
}
