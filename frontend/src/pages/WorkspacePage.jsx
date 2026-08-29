import { useState } from 'react'
import PageHeader from '../components/common/PageHeader'
import { Card, Button, Spinner, ErrorState, EmptyState, Tabs } from '../components/ui'
import { useWorkspace } from '../features/workspace/hooks/useWorkspace'
import WorkspaceMembers from '../features/workspace/components/WorkspaceMembers'
import CreateWorkspaceModal from '../features/workspace/components/CreateWorkspaceModal'
import '../features/workspace/components/workspace.css'

const TABS = [
  { key: 'overview', label: 'Overview' },
  { key: 'members', label: 'Members' },
]

export default function WorkspacePage() {
  const {
    workspaces,
    activeWorkspace,
    activeWorkspaceId,
    selectWorkspace,
    isLoading,
    error,
    refreshWorkspaces,
  } = useWorkspace()
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [activeTab, setActiveTab] = useState('overview')

  if (isLoading) {
    return <Spinner size={24} />
  }

  if (error) {
    return <ErrorState message={error} onRetry={refreshWorkspaces} />
  }

  return (
    <div>
      <PageHeader
        title="Workspaces"
        subtitle="Everything in BugLens is scoped to a workspace."
        action={<Button onClick={() => setIsCreateOpen(true)}>New workspace</Button>}
      />

      {workspaces.length === 0 ? (
        <EmptyState
          title="No workspaces yet"
          description="Create your first workspace to start tracking issues."
          action={
            <Button onClick={() => setIsCreateOpen(true)}>Create workspace</Button>
          }
        />
      ) : (
        <>
          <div className="workspace-grid" style={{ marginBottom: 32 }}>
            {workspaces.map((ws) => (
              <Card
                key={ws.id}
                className={`workspace-card ${
                  String(ws.id) === String(activeWorkspaceId) ? 'workspace-card-active' : ''
                }`}
                onClick={() => selectWorkspace(ws.id)}
              >
                <h3>{ws.name}</h3>
                {ws.description && (
                  <p className="workspace-card-desc">{ws.description}</p>
                )}
              </Card>
            ))}
          </div>

          {activeWorkspace && (
            <>
              <Tabs tabs={TABS} activeKey={activeTab} onChange={setActiveTab} />
              <div style={{ marginTop: 20 }}>
                {activeTab === 'overview' ? (
                  <Card>
                    <h3>{activeWorkspace.name}</h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 8 }}>
                      {activeWorkspace.description || 'No description yet.'}
                    </p>
                  </Card>
                ) : (
                  <WorkspaceMembers workspaceId={activeWorkspace.id} />
                )}
              </div>
            </>
          )}
        </>
      )}

      <CreateWorkspaceModal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} />
    </div>
  )
}
