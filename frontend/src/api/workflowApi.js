import axiosClient from './axiosClient'

/** { currentStatus, allowedTransitions } — the backend owns which moves are legal. */
export function getTransitions(issueId) {
  return axiosClient.get(`/issues/${issueId}/transitions`).then((res) => res.data)
}

/** An illegal target comes back as 422 listing the targets that would have been allowed. */
export function transitionIssue(issueId, targetStatus, comment) {
  return axiosClient
    .post(`/issues/${issueId}/transitions`, { targetStatus, comment })
    .then((res) => res.data)
}
