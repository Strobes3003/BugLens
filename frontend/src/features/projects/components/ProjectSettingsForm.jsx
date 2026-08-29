import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as projectApi from '../../../api/projectApi'
import {
  Button,
  Input,
  Spinner,
  ErrorState,
  ConfirmDialog,
  useToast,
} from '../../../components/ui'
import { ROUTES } from '../../../utils/constants'
import './project.css'

export default function ProjectSettingsForm({ projectId }) {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [project, setProject] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isSaving, setIsSaving] = useState(false)
  const [isDeleteOpen, setIsDeleteOpen] = useState(false)

  useEffect(() => {
    projectApi
      .getProject(projectId)
      .then(setProject)
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [projectId])

  function handleChange(event) {
    const { name, value } = event.target
    setProject((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSaving(true)
    try {
      const updated = await projectApi.updateProject(projectId, {
        name: project.name,
        description: project.description,
      })
      setProject(updated)
      showToast('Project settings saved', { variant: 'success' })
    } catch (err) {
      showToast(err.message, { variant: 'error' })
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete() {
    try {
      await projectApi.deleteProject(projectId)
      showToast('Project deleted', { variant: 'success' })
      navigate(ROUTES.PROJECTS, { replace: true })
    } catch (err) {
      showToast(err.message, { variant: 'error' })
      setIsDeleteOpen(false)
    }
  }

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
        <Spinner size={24} />
      </div>
    )
  }

  if (error || !project) {
    return <ErrorState message={error || 'Project not found.'} />
  }

  return (
    <>
      <form className="settings-form" onSubmit={handleSubmit}>
        <Input
          label="Project key"
          value={project.key}
          disabled
          hint="Project keys can't be changed after creation"
        />
        <Input
          label="Project name"
          name="name"
          value={project.name}
          onChange={handleChange}
          required
        />
        <Input
          label="Description"
          name="description"
          value={project.description || ''}
          onChange={handleChange}
        />
        <div style={{ display: 'flex', gap: 8 }}>
          <Button type="submit" isLoading={isSaving}>
            Save changes
          </Button>
          <Button
            type="button"
            variant="danger"
            onClick={() => setIsDeleteOpen(true)}
          >
            Delete project
          </Button>
        </div>
      </form>
      <ConfirmDialog
        isOpen={isDeleteOpen}
        title="Delete project"
        message={`Delete "${project.name}"? This cannot be undone.`}
        confirmLabel="Delete"
        isDanger
        onConfirm={handleDelete}
        onCancel={() => setIsDeleteOpen(false)}
      />
    </>
  )
}
