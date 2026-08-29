import { useCallback, useEffect, useState } from 'react'
import PageHeader from '../components/common/PageHeader'
import { Button, Spinner, ErrorState, EmptyState } from '../components/ui'
import * as projectApi from '../api/projectApi'
import { useWorkspace } from '../features/workspace/hooks/useWorkspace'
import ProjectCard from '../features/projects/components/ProjectCard'
import CreateProjectModal from '../features/projects/components/CreateProjectModal'
import '../features/projects/components/project.css'

export default function ProjectsPage() {
  const { activeWorkspaceId, isLoading: isWorkspaceLoading } = useWorkspace()
  const [projects, setProjects] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isCreateOpen, setIsCreateOpen] = useState(false)

  const loadProjects = useCallback(() => {
    if (!activeWorkspaceId) {
      setProjects([])
      setIsLoading(false)
      return
    }
    setIsLoading(true)
    setError(null)
    projectApi
      .getProjects(activeWorkspaceId)
      .then(setProjects)
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [activeWorkspaceId])

  useEffect(() => {
    loadProjects()
  }, [loadProjects])

  if (isWorkspaceLoading || isLoading) {
    return <Spinner size={24} />
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadProjects} />
  }

  if (!activeWorkspaceId) {
    return (
      <EmptyState
        title="Select a workspace"
        description="Choose or create a workspace to see its projects."
      />
    )
  }

  return (
    <div>
      <PageHeader
        title="Projects"
        subtitle="Projects group issues, components, and releases."
        action={<Button onClick={() => setIsCreateOpen(true)}>New project</Button>}
      />

      {projects.length === 0 ? (
        <EmptyState
          title="No projects yet"
          description="Create a project to start tracking issues in this workspace."
          action={
            <Button onClick={() => setIsCreateOpen(true)}>Create project</Button>
          }
        />
      ) : (
        <div className="project-grid">
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}

      <CreateProjectModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={(created) => setProjects((current) => [...current, created])}
      />
    </div>
  )
}
