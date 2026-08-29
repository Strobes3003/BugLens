import axiosClient from './axiosClient'

/** { issueId, issueKey, blockedBy, blocking } for one issue. */
export function getDependencies(issueId) {
  return axiosClient
    .get(`/issues/${issueId}/dependencies`)
    .then((res) => res.data)
}

/** Every edge in the project, for whole-graph rendering. */
export function getProjectGraph(projectId) {
  return axiosClient
    .get(`/projects/${projectId}/dependencies`)
    .then((res) => res.data)
}

/** The path issue is the blocker: this records "blockerIssueId blocks blockedIssueId". */
export function addDependency(blockerIssueId, blockedIssueId) {
  return axiosClient
    .post(`/issues/${blockerIssueId}/dependencies`, { blockedIssueId })
    .then((res) => res.data)
}

export function removeDependency(blockerIssueId, blockedIssueId) {
  return axiosClient
    .delete(`/issues/${blockerIssueId}/dependencies/${blockedIssueId}`)
    .then((res) => res.data)
}

/** { blastRadius, totalBlockers, directBlockers, directBlocked, hasBottleneck } */
export function getDependencyAnalysis(issueId) {
  return axiosClient
    .get(`/issues/${issueId}/dependency-analysis`)
    .then((res) => res.data)
}
