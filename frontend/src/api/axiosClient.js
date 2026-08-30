import axios from 'axios'
import { API_BASE_URL, AUTH_TOKEN_KEY } from '../utils/constants'
import { getStoredValue, setStoredValue } from '../utils/storage'
import { emitUnauthorized } from './authEvents'

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

axiosClient.interceptors.request.use((config) => {
  const token = getStoredValue(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Dropping the token here is not enough on its own: AuthProvider holds the
      // `user` object that gates every protected route, and it would happily keep
      // rendering them while each request went out anonymous. Announce the
      // rejection so the two halves of the session are torn down together.
      setStoredValue(AUTH_TOKEN_KEY, null)
      emitUnauthorized()
    }
    return Promise.reject(normalizeApiError(error))
  },
)

export function normalizeApiError(error) {
  const status = error.response?.status ?? null
  const data = error.response?.data
  const message =
    (typeof data === 'string' ? data : data?.message) ||
    error.message ||
    'Something went wrong. Please try again.'
  return { status, message, cause: error }
}

export default axiosClient
