import axiosClient from './axiosClient'

export function getWorkspaces() {
  return axiosClient.get('/workspaces').then((res) => res.data)
}

export function getWorkspace(workspaceId) {
  return axiosClient.get(`/workspaces/${workspaceId}`).then((res) => res.data)
}

export function createWorkspace({ name, description }) {
  return axiosClient
    .post('/workspaces', { name, description })
    .then((res) => res.data)
}

export function updateWorkspace(workspaceId, payload) {
  return axiosClient
    .patch(`/workspaces/${workspaceId}`, payload)
    .then((res) => res.data)
}

export function deleteWorkspace(workspaceId) {
  return axiosClient
    .delete(`/workspaces/${workspaceId}`)
    .then((res) => res.data)
}

export function getWorkspaceMembers(workspaceId) {
  return axiosClient
    .get(`/workspaces/${workspaceId}/members`)
    .then((res) => res.data)
}

export function inviteWorkspaceMember(workspaceId, { email, role }) {
  return axiosClient
    .post(`/workspaces/${workspaceId}/members`, { email, role })
    .then((res) => res.data)
}

export function updateWorkspaceMemberRole(workspaceId, memberId, role) {
  return axiosClient
    .patch(`/workspaces/${workspaceId}/members/${memberId}`, { role })
    .then((res) => res.data)
}

export function removeWorkspaceMember(workspaceId, memberId) {
  return axiosClient
    .delete(`/workspaces/${workspaceId}/members/${memberId}`)
    .then((res) => res.data)
}
