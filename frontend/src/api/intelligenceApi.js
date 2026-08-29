import axiosClient from './axiosClient'

export function getFixNext(projectId, limit = 10) {
  return axiosClient
    .get(`/projects/${projectId}/fix-next`, { params: { limit } })
    .then((res) => res.data)
}

export function getComponentHealth(componentId) {
  return axiosClient
    .get(`/components/${componentId}/health`)
    .then((res) => res.data)
}

export function getReleaseRisk(releaseId) {
  return axiosClient.get(`/releases/${releaseId}/risk`).then((res) => res.data)
}
