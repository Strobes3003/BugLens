package com.buglens.comment.repository;

import com.buglens.comment.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findAllByIssueIdOrderByCreatedAtAsc(Long issueId);

    long countByIssueId(Long issueId);
}
