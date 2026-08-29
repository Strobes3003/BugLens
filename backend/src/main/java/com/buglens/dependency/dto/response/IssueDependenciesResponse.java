package com.buglens.dependency.dto.response;

import java.util.List;

/**
 * Both directions of an issue's immediate neighbourhood.
 *
 * @param blockedBy issues that must be resolved before this one can proceed (incoming edges)
 * @param blocking  issues waiting on this one (outgoing edges)
 */
public record IssueDependenciesResponse(
        Long issueId,
        String issueKey,
        List<DependencyIssueSummary> blockedBy,
        List<DependencyIssueSummary> blocking
) {
}
