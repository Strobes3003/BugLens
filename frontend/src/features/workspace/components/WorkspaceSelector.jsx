import { useState } from 'react'
import { Dropdown, Avatar } from '../../../components/ui'
import { useWorkspace } from '../hooks/useWorkspace'
import CreateWorkspaceModal from './CreateWorkspaceModal'
import './workspace.css'

export default function WorkspaceSelector() {
  const { workspaces, activeWorkspace, selectWorkspace } = useWorkspace()
  const [isCreateOpen, setIsCreateOpen] = useState(false)

  const items = [
    ...workspaces.map((ws) => ({
      key: String(ws.id),
      label: ws.name,
      onClick: () => selectWorkspace(ws.id),
    })),
    { key: 'create', label: '+ New workspace', onClick: () => setIsCreateOpen(true) },
  ]

  return (
    <>
      <Dropdown
        align="left"
        trigger={
          <span className="workspace-trigger">
            <Avatar name={activeWorkspace?.name || '?'} size={24} />
            {activeWorkspace?.name || 'Select workspace'}
            <span className="workspace-trigger-chevron">▾</span>
          </span>
        }
        items={items}
      />
      <CreateWorkspaceModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
      />
    </>
  )
}
