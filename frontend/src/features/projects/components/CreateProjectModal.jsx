import { useState } from 'react'
import { Modal, Button, Input } from '../../../components/ui'
import * as projectApi from '../../../api/projectApi'
import { useWorkspace } from '../../workspace/hooks/useWorkspace'

export default function CreateProjectModal({ isOpen, onClose, onCreated }) {
  const { activeWorkspaceId } = useWorkspace()
  const [form, setForm] = useState({ name: '', key: '', description: '' })
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({
      ...current,
      [name]: name === 'key' ? value.toUpperCase() : value,
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const created = await projectApi.createProject(activeWorkspaceId, form)
      onCreated(created)
      setForm({ name: '', key: '', description: '' })
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create project"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="create-project-form" isLoading={isSubmitting}>
            Create
          </Button>
        </>
      }
    >
      <form
        id="create-project-form"
        onSubmit={handleSubmit}
        style={{ display: 'flex', flexDirection: 'column', gap: 14 }}
      >
        {error && <div className="form-error">{error}</div>}
        <Input
          label="Project name"
          name="name"
          value={form.name}
          onChange={handleChange}
          required
          autoFocus
        />
        <Input
          label="Project key"
          name="key"
          value={form.key}
          onChange={handleChange}
          hint="Short prefix used on issue IDs, e.g. CD"
          maxLength={10}
          required
        />
        <Input
          label="Description"
          name="description"
          value={form.description}
          onChange={handleChange}
          hint="Optional"
        />
      </form>
    </Modal>
  )
}
