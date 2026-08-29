package com.buglens.issue.repository;

import com.buglens.auth.entity.User;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import com.buglens.workspace.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IssueRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Project project;
    private Component component;
    private Release release;
    private User reporter;
    private User assignee;

    @BeforeEach
    void setUp() {
        reporter = persistUser("Reporter", "reporter@buglens.test");
        assignee = persistUser("Assignee", "assignee@buglens.test");

        Workspace workspace = entityManager.persist(
                new Workspace("Core Workspace", "core-workspace-" + System.nanoTime(), reporter)
        );
        project = entityManager.persist(
                new Project(workspace, "BugLens", "BL", "Issue tracker", ProjectStatus.ACTIVE)
        );
        component = entityManager.persist(
                new Component(project, "Authentication", "Login and sessions", ComponentStatus.ACTIVE)
        );
        release = entityManager.persist(
                new Release(project, "v2.4", "Q3 release", ReleaseStatus.ACTIVE, LocalDate.of(2026, 9, 30))
        );
        entityManager.flush();
    }

    @Test
    void savesAndRetrievesIssue() {
        Issue saved = issueRepository.saveAndFlush(newIssue("BL-101", "Login fails after password reset", assignee));
        entityManager.clear();

        Optional<Issue> found = issueRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        Issue issue = found.get();
        assertEquals("BL-101", issue.getIssueKey());
        assertEquals("Login fails after password reset", issue.getTitle());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertEquals(IssuePriority.HIGH, issue.getPriority());
        assertEquals(IssueSeverity.CRITICAL, issue.getSeverity());
        assertEquals(component.getId(), issue.getComponent().getId());
        assertEquals(release.getId(), issue.getRelease().getId());
        assertEquals(reporter.getId(), issue.getReporter().getId());
        assertEquals(assignee.getId(), issue.getAssignee().getId());
        assertNotNull(issue.getCreatedAt());
        assertNotNull(issue.getUpdatedAt());
    }

    @Test
    void savesIssueWithoutAssignee() {
        Issue saved = issueRepository.saveAndFlush(newIssue("BL-102", "Unassigned backlog item", null));
        entityManager.clear();

        Issue issue = issueRepository.findById(saved.getId()).orElseThrow();

        assertNull(issue.getAssignee());
    }

    @Test
    void rejectsIssueWithoutTitle() {
        Issue issue = newIssue("BL-103", null, assignee);

        assertThrows(DataIntegrityViolationException.class, () -> issueRepository.saveAndFlush(issue));
    }

    @Test
    void rejectsIssueWithBlankTitle() {
        Issue issue = newIssue("BL-104", "   ", assignee);

        assertThrows(DataIntegrityViolationException.class, () -> issueRepository.saveAndFlush(issue));
    }

    @Test
    void rejectsIssueWithoutComponent() {
        Issue issue = new Issue(
                "BL-107",
                "Missing component",
                null,
                IssueStatus.OPEN,
                IssuePriority.MEDIUM,
                IssueSeverity.MEDIUM,
                null,
                release,
                reporter,
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> issueRepository.saveAndFlush(issue));
    }

    @Test
    void savesBacklogIssueWithoutRelease() {
        Issue issue = new Issue(
                "BL-108",
                "Backlog item not yet scheduled",
                null,
                IssueStatus.OPEN,
                IssuePriority.MEDIUM,
                IssueSeverity.MEDIUM,
                component,
                null,
                reporter,
                null
        );

        Issue saved = issueRepository.saveAndFlush(issue);
        entityManager.clear();

        Issue found = issueRepository.findById(saved.getId()).orElseThrow();

        assertNull(found.getRelease());
        assertEquals("BL-108", found.getIssueKey());
    }

    @Test
    void rejectsDuplicateIssueKey() {
        issueRepository.saveAndFlush(newIssue("BL-109", "First issue", null));
        Issue duplicate = newIssue("BL-109", "Second issue with same key", null);

        assertThrows(DataIntegrityViolationException.class, () -> issueRepository.saveAndFlush(duplicate));
    }

    @Test
    void rejectsIssueWithoutIssueKey() {
        Issue issue = newIssue(null, "Missing issue key", null);

        assertThrows(DataIntegrityViolationException.class, () -> issueRepository.saveAndFlush(issue));
    }

    @Test
    void findsIssuesByComponentAndByProject() {
        issueRepository.saveAndFlush(newIssue("BL-105", "First issue", assignee));
        issueRepository.saveAndFlush(newIssue("BL-106", "Second issue", null));
        entityManager.clear();

        List<Issue> byComponent = issueRepository.findAllByComponentIdOrderByCreatedAtDesc(component.getId());
        List<Issue> byRelease = issueRepository.findAllByReleaseIdOrderByCreatedAtDesc(release.getId());
        List<Issue> byProject = issueRepository.findAllByComponentProjectIdOrderByCreatedAtDesc(project.getId());

        assertEquals(2, byComponent.size());
        assertEquals(2, byRelease.size());
        assertEquals(2, byProject.size());
        assertEquals(2, issueRepository.countByComponentIdAndStatus(component.getId(), IssueStatus.OPEN));
    }

    private Issue newIssue(String issueKey, String title, User issueAssignee) {
        return new Issue(
                issueKey,
                title,
                "Steps to reproduce are attached.",
                IssueStatus.OPEN,
                IssuePriority.HIGH,
                IssueSeverity.CRITICAL,
                component,
                release,
                reporter,
                issueAssignee
        );
    }

    private User persistUser(String name, String email) {
        return entityManager.persist(new User(name, email, "hashed-password", null));
    }
}
