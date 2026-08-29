package com.buglens.dependency.dto.response;

import java.util.List;

/**
 * Holistic view of one issue's position in the dependency graph.
 *
 * @param blastRadius   how many distinct issues are downstream of this one, directly or through
 *                      any chain. Counts nodes, not paths: an issue reachable by several routes
 *                      through a diamond is counted once.
 * @param totalBlockers how many distinct issues are upstream of this one, at any depth.
 * @param directBlockers  issues blocking this one right now (one hop upstream)
 * @param directBlocked   issues this one is blocking right now (one hop downstream)
 * @param hasBottleneck   whether this issue directly blocks enough others to be a chokepoint
 */
public record DependencyAnalysisResponse(
        Long issueId,
        String issueKey,
        int blastRadius,
        int totalBlockers,
        List<DependencyIssueSummary> directBlockers,
        List<DependencyIssueSummary> directBlocked,
        boolean hasBottleneck
) {
}
