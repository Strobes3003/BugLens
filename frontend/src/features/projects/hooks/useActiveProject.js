import { useCallback, useEffect, useState } from 'react'
import * as projectApi from '../../../api/projectApi'
import { useWorkspace } from '../../workspace/hooks/useWorkspace'
import { ACTIVE_PROJECT_KEY } from '../../../utils/constants'
import { getStoredValue, setStoredValue } from '../../../utils/storage'

/**
 * Loads the active workspace's projects and remembers which one the user is looking at.
 *
 * The issue, dependency and intelligence views are all project-scoped, but the app only tracks
 * an active workspace, so each of those pages needs a project before it can ask the backend for
 * anything. Selection is remembered across reloads and falls back to the first project.
 */
export function useActiveProject() {
  const { activeWorkspaceId, isLoading: isWorkspaceLoading } = useWorkspace()

  const [projects, setProjects] = useState([])
  const [activeProjectId, setActiveProjectId] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadProjects = useCallback(() => {
    if (!activeWorkspaceId) {
      setProjects([])
      setActiveProjectId(null)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    projectApi
      .getProjects(activeWorkspaceId)
      .then((loaded) => {
        setProjects(loaded)

        const remembered = getStoredValue(ACTIVE_PROJECT_KEY)
        const stillExists = loaded.some(
          (project) => String(project.id) === String(remembered),
        )
        const next = stillExists ? Number(remembered) : (loaded[0]?.id ?? null)

        setActiveProjectId(next)
        setStoredValue(ACTIVE_PROJECT_KEY, next === null ? null : String(next))
      })
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [activeWorkspaceId])

  useEffect(() => {
    loadProjects()
  }, [loadProjects])

  const selectProject = useCallback((projectId) => {
    const next = projectId === null ? null : Number(projectId)
    setActiveProjectId(next)
    setStoredValue(ACTIVE_PROJECT_KEY, next === null ? null : String(next))
  }, [])

  return {
    projects,
    activeProjectId,
    activeProject: projects.find((p) => p.id === activeProjectId) ?? null,
    selectProject,
    isLoading: isWorkspaceLoading || isLoading,
    error,
    reload: loadProjects,
  }
}
