package com.buglens.dependency.repository;

import com.buglens.auth.entity.User;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.dependency.entity.IssueDependency;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
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
class IssueDependencyRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private IssueDependencyRepository dependencyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Issue issueA;
    private Issue issueB;
    private Issue issueC;
    private Issue issueD;
    private Issue issueE;
    private Project project;

    @BeforeEach
    void setUp() {
        User user = entityManager.persist(
                new User("Actor", "actor-" + System.nanoTime() + "@buglens.test", "hash", null)
        );
        Workspace workspace = entityManager.persist(
                new Workspace("Workspace", "ws-" + System.nanoTime(), user)
        );
        project = entityManager.persist(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE)
        );
        Component component = entityManager.persist(
                new Component(project, "Auth", null, ComponentStatus.ACTIVE)
        );

        issueA = persistIssue(component, user, "BL-1");
        issueB = persistIssue(component, user, "BL-2");
        issueC = persistIssue(component, user, "BL-3");
        issueD = persistIssue(component, user, "BL-4");
        issueE = persistIssue(component, user, "BL-5");
        entityManager.flush();
    }

    @Test
    void savesDirectedEdgeAndReadsBothDirections() {
        dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueB));
        entityManager.clear();

        List<IssueDependency> outgoingFromA =
                dependencyRepository.findAllByBlockingIdOrderByCreatedAtAsc(issueA.getId());
        List<IssueDependency> incomingToB =
                dependencyRepository.findAllByBlockedIdOrderByCreatedAtAsc(issueB.getId());

        assertEquals(1, outgoingFromA.size());
        assertEquals(1, incomingToB.size());
        assertEquals(List.of(issueB.getId()), dependencyRepository.findBlockedIssueIds(issueA.getId()));
        assertEquals(List.of(), dependencyRepository.findBlockedIssueIds(issueB.getId()));
    }

    @Test
    void rejectsDuplicateEdgeAtDatabaseLevel() {
        dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueB));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueB))
        );
    }

    @Test
    void rejectsSelfEdgeAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueA))
        );
    }

    @Test
    void allowsOppositeDirectionAtDatabaseLevel() {
        dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueB));

        // The DB only enforces uniqueness of the ordered pair; B -> A is a distinct row and is
        // rejected by the cycle detector in the service, not by the schema.
        dependencyRepository.saveAndFlush(new IssueDependency(issueB, issueA));

        assertEquals(2, dependencyRepository.count());
    }

    /**
     * The cascade lives in the foreign keys, so this deletes and counts through native SQL:
     * going via the repository would only show Hibernate's persistence context reacting.
     */
    @Test
    void deletingIssueCascadesBothIncomingAndOutgoingEdges() {
        dependencyRepository.saveAndFlush(new IssueDependency(issueA, issueB));
        dependencyRepository.saveAndFlush(new IssueDependency(issueB, issueC));
        assertEquals(2, dependencyRepository.count());

        Long removedId = issueB.getId();
        entityManager.flush();
        entityManager.clear();

        entityManager.getEntityManager()
                .createNativeQuery("DELETE FROM issues WHERE id = :id")
                .setParameter("id", removedId)
                .executeUpdate();

        Number remaining = (Number) entityManager.getEntityManager()
                .createNativeQuery(
                        "SELECT COUNT(*) FROM issue_dependencies "
                                + "WHERE blocking_issue_id = :id OR blocked_issue_id = :id")
                .setParameter("id", removedId)
                .getSingleResult();

        assertEquals(0L, remaining.longValue());
    }

    // ---------- recursive CTE traversal, executed by PostgreSQL ----------

    @Test
    void findsPathAcrossDeepChain() {
        edge(issueA, issueB);
        edge(issueB, issueC);
        edge(issueC, issueD);
        edge(issueD, issueE);
        entityManager.flush();

        String path = dependencyRepository
                .findPathBetween(issueA.getId(), issueE.getId())
                .orElseThrow();

        assertEquals(
                List.of(issueA.getId(), issueB.getId(), issueC.getId(), issueD.getId(), issueE.getId()),
                parse(path)
        );
    }

    @Test
    void returnsNoPathWhenTargetIsNotReachable() {
        edge(issueA, issueB);
        edge(issueC, issueD);
        entityManager.flush();

        assertTrue(dependencyRepository.findPathBetween(issueA.getId(), issueD.getId()).isEmpty());
    }

    @Test
    void findsNoPathUpstreamBecauseTheGraphIsDirected() {
        edge(issueA, issueB);
        entityManager.flush();

        assertTrue(dependencyRepository.findPathBetween(issueA.getId(), issueB.getId()).isPresent());
        assertTrue(dependencyRepository.findPathBetween(issueB.getId(), issueA.getId()).isEmpty());
    }

    @Test
    void reportsShortestHopPathThroughADiamond() {
        edge(issueA, issueB);
        edge(issueA, issueC);
        edge(issueB, issueD);
        edge(issueC, issueD);
        entityManager.flush();

        List<Long> path = parse(dependencyRepository
                .findPathBetween(issueA.getId(), issueD.getId())
                .orElseThrow());

        assertEquals(issueA.getId(), path.get(0));
        assertEquals(issueD.getId(), path.get(path.size() - 1));
        assertEquals(3, path.size());
    }

    /**
     * The recursion guard must hold even if a cycle is already present in the data — otherwise
     * the query would never terminate. The cycle is inserted directly, bypassing the service.
     */
    @Test
    void terminatesWhenTheStoredGraphAlreadyContainsACycle() {
        edge(issueA, issueB);
        edge(issueB, issueC);
        edge(issueC, issueA);
        entityManager.flush();

        assertTrue(dependencyRepository.findPathBetween(issueA.getId(), issueC.getId()).isPresent());
        assertTrue(dependencyRepository.findPathBetween(issueA.getId(), issueE.getId()).isEmpty());
    }

    @Test
    void findsSelfPathForAnIsolatedNode() {
        assertEquals(
                List.of(issueA.getId()),
                parse(dependencyRepository.findPathBetween(issueA.getId(), issueA.getId()).orElseThrow())
        );
    }

    // ---------- advisory lock ----------

    @Test
    void acquiresProjectGraphLock() {
        assertEquals(Boolean.TRUE, dependencyRepository.lockProjectGraph(project.getId()));
        // Re-entrant for the same transaction; a second acquisition must not deadlock.
        assertEquals(Boolean.TRUE, dependencyRepository.lockProjectGraph(project.getId()));
    }

    private void edge(Issue blocking, Issue blocked) {
        dependencyRepository.saveAndFlush(new IssueDependency(blocking, blocked));
    }

    private List<Long> parse(String path) {
        return java.util.Arrays.stream(path.split(",")).map(Long::valueOf).toList();
    }

    private Issue persistIssue(Component component, User reporter, String key) {
        return entityManager.persist(new Issue(
                key, "Issue " + key, null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH,
                component, null, reporter, null
        ));
    }
}
