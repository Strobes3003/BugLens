package com.buglens.issue.repository;

import com.buglens.auth.entity.User;
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
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IssueSequenceRepository.class)
@Testcontainers
class IssueSequenceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private IssueSequenceRepository issueSequenceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Project project;
    private Project otherProject;

    @BeforeEach
    void setUp() {
        User creator = entityManager.persist(
                new User("Creator", "creator-" + System.nanoTime() + "@buglens.test", "hash", null)
        );
        Workspace workspace = entityManager.persist(
                new Workspace("Workspace", "workspace-" + System.nanoTime(), creator)
        );
        project = entityManager.persist(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE)
        );
        otherProject = entityManager.persist(
                new Project(workspace, "Other", "OT", null, ProjectStatus.ACTIVE)
        );
        entityManager.flush();
    }

    @Test
    void allocatesConsecutiveNumbersStartingAtOne() {
        assertEquals(1L, issueSequenceRepository.allocateNext(project.getId()));
        assertEquals(2L, issueSequenceRepository.allocateNext(project.getId()));
        assertEquals(3L, issueSequenceRepository.allocateNext(project.getId()));
    }

    @Test
    void allocatesIndependentlyPerProject() {
        assertEquals(1L, issueSequenceRepository.allocateNext(project.getId()));
        assertEquals(2L, issueSequenceRepository.allocateNext(project.getId()));

        assertEquals(1L, issueSequenceRepository.allocateNext(otherProject.getId()));
        assertEquals(3L, issueSequenceRepository.allocateNext(project.getId()));
    }
}
