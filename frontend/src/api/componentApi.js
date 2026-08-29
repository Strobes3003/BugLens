import axiosClient from './axiosClient'

/** Components are listed by query parameter, not nested under the project. */
export function getComponents(projectId) {
  return axiosClient
    .get('/components', { params: { projectId } })
    .then((res) => res.data)
}
