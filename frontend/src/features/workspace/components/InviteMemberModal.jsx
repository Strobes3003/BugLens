import { useState } from 'react'
import { Modal, Button, Input } from '../../../components/ui'
import { WORKSPACE_ROLES } from '../../../utils/constants'

export default function InviteMemberModal({ isOpen, onClose, onInvite }) {
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('MEMBER')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await onInvite({ email, role })
      setEmail('')
      setRole('MEMBER')
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
      title="Invite member"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="invite-member-form" isLoading={isSubmitting}>
            Send invite
          </Button>
        </>
      }
    >
      <form
        id="invite-member-form"
        onSubmit={handleSubmit}
        style={{ display: 'flex', flexDirection: 'column', gap: 14 }}
      >
        {error && <div className="form-error">{error}</div>}
        <Input
          label="Email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          autoFocus
        />
        <div className="field">
          <label className="field-label" htmlFor="invite-role">
            Role
          </label>
          <select
            id="invite-role"
            className="field-input"
            value={role}
            onChange={(event) => setRole(event.target.value)}
          >
            {WORKSPACE_ROLES.filter((r) => r !== 'OWNER').map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
      </form>
    </Modal>
  )
}
