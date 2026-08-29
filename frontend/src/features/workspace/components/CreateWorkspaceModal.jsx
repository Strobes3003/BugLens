import { useState } from 'react'
import { Modal, Button, Input } from '../../../components/ui'
import { useWorkspace } from '../hooks/useWorkspace'
import { useToast } from '../../../components/ui'

export default function CreateWorkspaceModal({ isOpen, onClose }) {
  const { createWorkspace } = useWorkspace()
  const { showToast } = useToast()
  const [form, setForm] = useState({ name: '', description: '' })
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await createWorkspace(form)
      showToast('Workspace created', { variant: 'success' })
      setForm({ name: '', description: '' })
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
      title="Create workspace"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="create-workspace-form"
            isLoading={isSubmitting}
          >
            Create
          </Button>
        </>
      }
    >
      <form
        id="create-workspace-form"
        onSubmit={handleSubmit}
        style={{ display: 'flex', flexDirection: 'column', gap: 14 }}
      >
        {error && <div className="form-error">{error}</div>}
        <Input
          label="Workspace name"
          name="name"
          value={form.name}
          onChange={handleChange}
          required
          autoFocus
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
