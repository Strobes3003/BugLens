import axiosClient from './axiosClient'

export function getComments(issueId) {
  return axiosClient.get(`/issues/${issueId}/comments`).then((res) => res.data)
}

export function createComment(issueId, body) {
  return axiosClient
    .post(`/issues/${issueId}/comments`, { body })
    .then((res) => res.data)
}

export function updateComment(commentId, body) {
  return axiosClient
    .patch(`/comments/${commentId}`, { body })
    .then((res) => res.data)
}

export function deleteComment(commentId) {
  return axiosClient.delete(`/comments/${commentId}`).then((res) => res.data)
}
