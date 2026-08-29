import { useCallback, useEffect, useMemo, useState } from 'react'
import * as workspaceApi from '../../../api/workspaceApi'
import { WorkspaceContext } from './WorkspaceContext'
import { ACTIVE_WORKSPACE_KEY } from '../../../utils/constants'
import { getStoredValue, setStoredValue } from '../../../utils/storage'

// Only ever mounted inside ProtectedRoute, so the user is always authenticated here.
export function WorkspaceProvider({ children }) {
  const [workspaces, setWorkspaces] = useState([])
  const [activeWorkspaceId, setActiveWorkspaceId] = useState(() =>
    getStoredValue(ACTIVE_WORKSPACE_KEY),
  )
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadWorkspaces = useCallback(() => {
    setIsLoading(true)
    setError(null)
    return workspaceApi
      .getWorkspaces()
      .then((data) => {
        setWorkspaces(data)
        setActiveWorkspaceId((current) => {
          if (current && data.some((ws) => String(ws.id) === String(current))) {
            return current
          }
          return data[0]?.id ?? null
        })
      })
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(() => {
    loadWorkspaces()
  }, [loadWorkspaces])

  useEffect(() => {
    setStoredValue(ACTIVE_WORKSPACE_KEY, activeWorkspaceId)
  }, [activeWorkspaceId])

  const createWorkspace = useCallback(async (payload) => {
    const created = await workspaceApi.createWorkspace(payload)
    setWorkspaces((current) => [...current, created])
    setActiveWorkspaceId(created.id)
    return created
  }, [])

  const activeWorkspace = useMemo(
    () => workspaces.find((ws) => String(ws.id) === String(activeWorkspaceId)) || null,
    [workspaces, activeWorkspaceId],
  )

  const value = useMemo(
    () => ({
      workspaces,
      activeWorkspace,
      activeWorkspaceId,
      selectWorkspace: setActiveWorkspaceId,
      createWorkspace,
      refreshWorkspaces: loadWorkspaces,
      isLoading,
      error,
    }),
    [
      workspaces,
      activeWorkspace,
      activeWorkspaceId,
      createWorkspace,
      loadWorkspaces,
      isLoading,
      error,
    ],
  )

  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  )
}
