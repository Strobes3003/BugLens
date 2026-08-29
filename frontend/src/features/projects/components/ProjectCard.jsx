import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui'
import './project.css'

export default function ProjectCard({ project }) {
  return (
    <Link
      to={`/projects/${project.id}/settings`}
      style={{ textDecoration: 'none' }}
    >
      <Card className="project-card">
        <span className="project-card-key">{project.key}</span>
        <h3>{project.name}</h3>
        {project.description && (
          <p className="project-card-desc">{project.description}</p>
        )}
      </Card>
    </Link>
  )
}
