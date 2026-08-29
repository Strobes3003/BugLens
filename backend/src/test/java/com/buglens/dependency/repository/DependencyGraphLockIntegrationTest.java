package com.buglens.dependency.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the advisory lock actually excludes a second writer, rather than merely being callable.
 *
 * <p>Uses two raw connections because that is the only way to observe mutual exclusion: a single
 * transaction re-entering its own advisory lock succeeds by design and would prove nothing. The
 * second connection sets a short {@code lock_timeout} so a successful block surfaces as a prompt
 * error instead of hanging the suite.
 */
@SpringBootTest
class DependencyGraphLockIntegrationTest {

    private static final long PROJECT_KEY = 987_654L;

    @Autowired
    private DataSource dataSource;

    @Test
    void secondWriterBlocksWhileFirstHoldsTheProjectLock() throws SQLException {
        try (Connection first = dataSource.getConnection();
             Connection second = dataSource.getConnection()) {

            first.setAutoCommit(false);
            second.setAutoCommit(false);

            try (Statement statement = first.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(" + PROJECT_KEY + ")");
            }

            try (Statement statement = second.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '500ms'");
                assertThrows(
                        SQLException.class,
                        () -> statement.execute("SELECT pg_advisory_xact_lock(" + PROJECT_KEY + ")"),
                        "second writer should have been blocked by the held advisory lock"
                );
            }

            second.rollback();

            // Releasing the first transaction lets the next writer straight through.
            first.rollback();

            try (Statement statement = second.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '2s'");
                assertDoesNotThrow(
                        () -> statement.execute("SELECT pg_advisory_xact_lock(" + PROJECT_KEY + ")")
                );
            }
            second.rollback();
        }
    }

    @Test
    void differentProjectsDoNotBlockEachOther() throws SQLException {
        try (Connection first = dataSource.getConnection();
             Connection second = dataSource.getConnection()) {

            first.setAutoCommit(false);
            second.setAutoCommit(false);

            try (Statement statement = first.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(" + PROJECT_KEY + ")");
            }

            try (Statement statement = second.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '2s'");
                assertDoesNotThrow(
                        () -> statement.execute("SELECT pg_advisory_xact_lock(" + (PROJECT_KEY + 1) + ")")
                );
            }

            first.rollback();
            second.rollback();
        }
    }
}
