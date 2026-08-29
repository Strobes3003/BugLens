package com.buglens.dependency.exception;

import java.util.List;

/**
 * Raised when adding an edge would close a loop in the dependency graph.
 *
 * <p>Carries the offending path so the caller is told which chain already exists, rather than
 * just that "something" was circular.
 */
public class CycleDetectedException extends RuntimeException {

    private final transient List<Long> cyclePath;

    public CycleDetectedException(Long blockingIssueId, Long blockedIssueId, List<Long> cyclePath) {
        super(buildMessage(blockingIssueId, blockedIssueId, cyclePath));
        this.cyclePath = List.copyOf(cyclePath);
    }

    public List<Long> getCyclePath() {
        return cyclePath;
    }

    private static String buildMessage(Long blockingIssueId, Long blockedIssueId, List<Long> cyclePath) {
        StringBuilder message = new StringBuilder()
                .append("Issue ").append(blockingIssueId)
                .append(" cannot block issue ").append(blockedIssueId)
                .append(": issue ").append(blockedIssueId)
                .append(" already blocks it");
        if (!cyclePath.isEmpty()) {
            message.append(" via ").append(cyclePath);
        }
        return message.toString();
    }
}
