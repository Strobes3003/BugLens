import { useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '../../../api/authApi'
import { onUnauthorized } from '../../../api/authEvents'
import { AuthContext } from './AuthContext'
import { AUTH_TOKEN_KEY } from '../../../utils/constants'
import { getStoredValue, setStoredValue } from '../../../utils/storage'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(() => Boolean(getStoredValue(AUTH_TOKEN_KEY)))
  const [error, setError] = useState(null)

  useEffect(() => {
    const token = getStoredValue(AUTH_TOKEN_KEY)
    if (!token) {
      return
    }
    authApi
      .getCurrentUser()
      .then((currentUser) => setUser(currentUser))
      .catch(() => setStoredValue(AUTH_TOKEN_KEY, null))
      .finally(() => setIsLoading(false))
  }, [])

  // A 401 from any call means the stored token is gone or no longer accepted.
  // Clear the user too, so ProtectedRoute sends us back to the login screen
  // rather than leaving a signed-in-looking shell firing anonymous requests.
  useEffect(() => onUnauthorized(() => setUser(null)), [])

  const login = useCallback(async (credentials) => {
    setError(null)
    try {
      const { accessToken, user: loggedInUser } = await authApi.login(credentials)
      setStoredValue(AUTH_TOKEN_KEY, accessToken)
      setUser(loggedInUser)
      return loggedInUser
    } catch (err) {
      setError(err.message)
      throw err
    }
  }, [])

  const register = useCallback(async (payload) => {
    setError(null)
    try {
      const { accessToken, user: registeredUser } = await authApi.register(payload)
      setStoredValue(AUTH_TOKEN_KEY, accessToken)
      setUser(registeredUser)
      return registeredUser
    } catch (err) {
      setError(err.message)
      throw err
    }
  }, [])

  // Nothing to revoke server-side: the JWT is stateless, so dropping it locally is the logout.
  const logout = useCallback(() => {
    setStoredValue(AUTH_TOKEN_KEY, null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      error,
      login,
      register,
      logout,
    }),
    [user, isLoading, error, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
