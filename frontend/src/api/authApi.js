import axiosClient from './axiosClient'

export function login({ email, password }) {
  return axiosClient
    .post('/auth/login', { email, password })
    .then((res) => res.data)
}

export function register({ name, email, password }) {
  return axiosClient
    .post('/auth/register', { name, email, password })
    .then((res) => res.data)
}

export function getCurrentUser() {
  return axiosClient.get('/auth/me').then((res) => res.data)
}
