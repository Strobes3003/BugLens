import axiosClient from './axiosClient'

/** Releases are listed by query parameter, not nested under the project. */
export function getReleases(projectId) {
  return axiosClient
    .get('/releases', { params: { projectId } })
    .then((res) => res.data)
}
