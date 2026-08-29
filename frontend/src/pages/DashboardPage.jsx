import { Link } from 'react-router-dom'
import PageHeader from '../components/common/PageHeader'
import { Card, Spinner } from '../components/ui'
import { useAuth } from '../features/auth/hooks/useAuth'
import { useWorkspace } from '../features/workspace/hooks/useWorkspace'
import { ROUTES } from '../utils/constants'

export default function DashboardPage() {
  const { user } = useAuth()
  const { activeWorkspace, isLoading } = useWorkspace()

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${user?.name?.split(' ')[0] || 'there'}`}
        subtitle={
          activeWorkspace
            ? `You're viewing ${activeWorkspace.name}`
            : 'Create a workspace to get started'
        }
      />

      {isLoading ? (
        <Spinner size={24} />
      ) : (
        <div style={{ display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
          <Link to={ROUTES.PROJECTS} style={{ textDecoration: 'none' }}>
            <Card>
              <h3>Projects</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 6 }}>
                Browse and manage this workspace&apos;s projects.
              </p>
            </Card>
          </Link>
          <Link to={ROUTES.WORKSPACE} style={{ textDecoration: 'none' }}>
            <Card>
              <h3>Workspace</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 6 }}>
                Manage members, roles, and workspace settings.
              </p>
            </Card>
          </Link>
        </div>
      )}
    </div>
  )
}
