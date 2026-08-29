import { useCallback, useEffect, useState } from 'react'
import * as workspaceApi from '../../../api/workspaceApi'
import {
  Table,
  Badge,
  Avatar,
  Button,
  Dropdown,
  ConfirmDialog,
  Spinner,
  ErrorState,
  useToast,
} from '../../../components/ui'
import InviteMemberModal from './InviteMemberModal'
import { WORKSPACE_ROLES } from '../../../utils/constants'

export default function WorkspaceMembers({ workspaceId }) {
  const { showToast } = useToast()
  const [members, setMembers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isInviteOpen, setIsInviteOpen] = useState(false)
  const [memberPendingRemoval, setMemberPendingRemoval] = useState(null)

  const loadMembers = useCallback(() => {
    if (!workspaceId) return
    setIsLoading(true)
    setError(null)
    workspaceApi
      .getWorkspaceMembers(workspaceId)
      .then(setMembers)
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [workspaceId])

  useEffect(() => {
    loadMembers()
  }, [loadMembers])

  async function handleInvite({ email, role }) {
    await workspaceApi.inviteWorkspaceMember(workspaceId, { email, role })
    showToast('Invitation sent', { variant: 'success' })
    loadMembers()
  }

  async function handleRoleChange(memberId, role) {
    try {
      await workspaceApi.updateWorkspaceMemberRole(workspaceId, memberId, role)
      loadMembers()
    } catch (err) {
      showToast(err.message, { variant: 'error' })
    }
  }

  async function handleRemove() {
    try {
      await workspaceApi.removeWorkspaceMember(workspaceId, memberPendingRemoval.id)
      showToast('Member removed', { variant: 'success' })
      setMemberPendingRemoval(null)
      loadMembers()
    } catch (err) {
      showToast(err.message, { variant: 'error' })
    }
  }

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
        <Spinner size={24} />
      </div>
    )
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadMembers} />
  }

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          marginBottom: 16,
        }}
      >
        <Button size="sm" onClick={() => setIsInviteOpen(true)}>
          Invite member
        </Button>
      </div>
      <Table
        columns={[
          {
            key: 'name',
            header: 'Member',
            render: (member) => (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Avatar name={member.name} size={28} />
                {member.name}
              </div>
            ),
          },
          { key: 'email', header: 'Email' },
          {
            key: 'role',
            header: 'Role',
            render: (member) =>
              member.role === 'OWNER' ? (
                <Badge variant="accent">OWNER</Badge>
              ) : (
                <Dropdown
                  trigger={<Badge variant="default">{member.role} ▾</Badge>}
                  items={WORKSPACE_ROLES.filter((r) => r !== 'OWNER').map(
                    (role) => ({
                      key: role,
                      label: role,
                      onClick: () => handleRoleChange(member.id, role),
                    }),
                  )}
                />
              ),
          },
          {
            key: 'actions',
            header: '',
            render: (member) =>
              member.role !== 'OWNER' && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setMemberPendingRemoval(member)}
                >
                  Remove
                </Button>
              ),
          },
        ]}
        data={members}
        emptyMessage="No members yet. Invite someone to collaborate."
      />
      <InviteMemberModal
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
        onInvite={handleInvite}
      />
      <ConfirmDialog
        isOpen={Boolean(memberPendingRemoval)}
        title="Remove member"
        message={`Remove ${memberPendingRemoval?.name} from this workspace?`}
        confirmLabel="Remove"
        isDanger
        onConfirm={handleRemove}
        onCancel={() => setMemberPendingRemoval(null)}
      />
    </div>
  )
}
