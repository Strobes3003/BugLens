import axiosClient from './axiosClient'

/** Chronological history for an issue: creations, updates, status changes, comments. */
export function getActivity(issueId) {
  return axiosClient.get(`/issues/${issueId}/activity`).then((res) => res.data)
}
