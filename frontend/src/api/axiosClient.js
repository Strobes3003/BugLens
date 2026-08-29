import axios from 'axios'
import { API_BASE_URL, AUTH_TOKEN_KEY } from '../utils/constants'
import { getStoredValue, setStoredValue } from '../utils/storage'

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
      setStoredValue(AUTH_TOKEN_KEY, null)
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
