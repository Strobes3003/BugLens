package com.buglens.comment.repository;

import com.buglens.auth.entity.User;
import com.buglens.comment.entity.Comment;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.workspace.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CommentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Issue issue;
    private User author;

    @BeforeEach
    void setUp() {
        author = entityManager.persist(
                new User("Author", "author-" + System.nanoTime() + "@buglens.test", "hash", null)
        );
        Workspace workspace = entityManager.persist(
                new Workspace("Workspace", "ws-" + System.nanoTime(), author)
        );
        Project project = entityManager.persist(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE)
        );
        Component component = entityManager.persist(
                new Component(project, "Auth", null, ComponentStatus.ACTIVE)
        );
        issue = entityManager.persist(new Issue(
                "BL-1", "Issue", null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH,
                component, null, author, null
        ));
        entityManager.flush();
    }

    @Test
    void savesAndListsCommentsChronologically() {
        commentRepository.saveAndFlush(new Comment(issue, author, "first"));
        commentRepository.saveAndFlush(new Comment(issue, author, "second"));
        entityManager.clear();

        List<Comment> comments = commentRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.getId());

        assertEquals(2, comments.size());
        assertEquals("first", comments.get(0).getBody());
        assertTrue(comments.stream().noneMatch(Comment::isEdited));
    }

    @Test
    void rejectsBlankBodyAtDatabaseLevel() {
        Comment blank = new Comment(issue, author, "   ");

        assertThrows(DataIntegrityViolationException.class, () -> commentRepository.saveAndFlush(blank));
    }

    /**
     * The cascade lives in the foreign key, not in JPA, so this deletes through native SQL and
     * counts through native SQL. Going via the repository would only prove that Hibernate holds
     * the comment in its persistence context, which is not what {@code ON DELETE CASCADE} means.
     */
    @Test
    void deletingIssueCascadeDeletesItsComments() {
        commentRepository.saveAndFlush(new Comment(issue, author, "will be cascaded"));
        Long issueId = issue.getId();
        assertEquals(1, commentRepository.countByIssueId(issueId));

        entityManager.flush();
        entityManager.clear();

        entityManager.getEntityManager()
                .createNativeQuery("DELETE FROM issues WHERE id = :id")
                .setParameter("id", issueId)
                .executeUpdate();

        Number remainingComments = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM comments WHERE issue_id = :id")
                .setParameter("id", issueId)
                .getSingleResult();

        assertEquals(0L, remainingComments.longValue());
    }

    @Test
    void deletingAuthorIsRestrictedWhileCommentsExist() {
        commentRepository.saveAndFlush(new Comment(issue, author, "blocks author deletion"));
        entityManager.flush();
        entityManager.clear();

        assertThrows(Exception.class, () -> {
            entityManager.getEntityManager()
                    .createNativeQuery("DELETE FROM users WHERE id = :id")
                    .setParameter("id", author.getId())
                    .executeUpdate();
            entityManager.flush();
        });
    }
}
