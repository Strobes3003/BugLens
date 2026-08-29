import { useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '../../../api/authApi'
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

  const login = useCallback(async (credentials) => {
    setError(null)
    try {
      const { token, user: loggedInUser } = await authApi.login(credentials)
      setStoredValue(AUTH_TOKEN_KEY, token)
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
      const { token, user: registeredUser } = await authApi.register(payload)
      setStoredValue(AUTH_TOKEN_KEY, token)
      setUser(registeredUser)
      return registeredUser
    } catch (err) {
      setError(err.message)
      throw err
    }
  }, [])

  const logout = useCallback(() => {
    setStoredValue(AUTH_TOKEN_KEY, null)
    setUser(null)
    authApi.logout().catch(() => {})
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
