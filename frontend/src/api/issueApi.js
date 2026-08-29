import axiosClient from './axiosClient'

export function getIssues(projectId, params = {}) {
  return axiosClient
    .get(`/projects/${projectId}/issues`, { params })
    .then((res) => res.data)
}

export function getIssue(issueId) {
  return axiosClient.get(`/issues/${issueId}`).then((res) => res.data)
}

export function getIssueByKey(issueKey) {
  return axiosClient.get(`/issues/key/${issueKey}`).then((res) => res.data)
}

export function createIssue(payload) {
  return axiosClient.post('/issues', payload).then((res) => res.data)
}

/** Details only. Status is owned by the workflow engine and is not accepted here. */
export function updateIssue(issueId, payload) {
  return axiosClient.patch(`/issues/${issueId}`, payload).then((res) => res.data)
}

export function deleteIssue(issueId) {
  return axiosClient.delete(`/issues/${issueId}`).then((res) => res.data)
}
