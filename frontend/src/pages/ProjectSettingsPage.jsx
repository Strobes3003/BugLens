import { useParams, Link } from 'react-router-dom'
import PageHeader from '../components/common/PageHeader'
import ProjectSettingsForm from '../features/projects/components/ProjectSettingsForm'
import { ROUTES } from '../utils/constants'

export default function ProjectSettingsPage() {
  const { projectId } = useParams()

  return (
    <div>
      <PageHeader
        title="Project settings"
        subtitle={
          <Link to={ROUTES.PROJECTS} style={{ color: 'var(--accent)' }}>
            ← Back to projects
          </Link>
        }
      />
      <ProjectSettingsForm projectId={projectId} />
    </div>
  )
}
