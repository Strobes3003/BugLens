import axiosClient from './axiosClient'

export function getProjects(workspaceId) {
  return axiosClient
    .get('/projects', { params: { workspaceId } })
    .then((res) => res.data)
}

export function getProject(projectId) {
  return axiosClient.get(`/projects/${projectId}`).then((res) => res.data)
}

export function createProject(workspaceId, { name, key, description }) {
  return axiosClient
    .post('/projects', { workspaceId, name, key, description })
    .then((res) => res.data)
}

export function updateProject(projectId, payload) {
  return axiosClient
    .patch(`/projects/${projectId}`, payload)
    .then((res) => res.data)
}

export function deleteProject(projectId) {
  return axiosClient.delete(`/projects/${projectId}`).then((res) => res.data)
}

export function getProjectSettings(projectId) {
  return axiosClient
    .get(`/projects/${projectId}/settings`)
    .then((res) => res.data)
}

export function updateProjectSettings(projectId, payload) {
  return axiosClient
    .put(`/projects/${projectId}/settings`, payload)
    .then((res) => res.data)
}

export function getProjectComponents(projectId) {
  return axiosClient
    .get(`/projects/${projectId}/components`)
    .then((res) => res.data)
}

export function createProjectComponent(projectId, { name, description }) {
  return axiosClient
    .post(`/projects/${projectId}/components`, { name, description })
    .then((res) => res.data)
}

export function getProjectReleases(projectId) {
  return axiosClient
    .get(`/projects/${projectId}/releases`)
    .then((res) => res.data)
}

export function createProjectRelease(projectId, { name, targetDate }) {
  return axiosClient
    .post(`/projects/${projectId}/releases`, { name, targetDate })
    .then((res) => res.data)
}
