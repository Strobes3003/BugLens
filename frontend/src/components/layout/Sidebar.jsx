import { NavLink } from 'react-router-dom'
import { ROUTES } from '../../utils/constants'
import './layout.css'

const PRIMARY_LINKS = [
  { to: ROUTES.DASHBOARD, label: 'Dashboard', icon: '🏠' },
  { to: ROUTES.WORKSPACE, label: 'Workspace', icon: '🧭' },
  { to: ROUTES.PROJECTS, label: 'Projects', icon: '📁' },
]

// Owned by Frontend B — routes land here once that module ships.
const UPCOMING_LINKS = [
  { label: 'Issues', icon: '🐞' },
  { label: 'Dependencies', icon: '🔗' },
  { label: 'Fix Next', icon: '🎯' },
  { label: 'Release Risk', icon: '📊' },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        {PRIMARY_LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`
            }
          >
            <span aria-hidden="true">{link.icon}</span>
            {link.label}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-section-label">Coming soon</div>
      <nav className="sidebar-nav">
        {UPCOMING_LINKS.map((link) => (
          <span key={link.label} className="sidebar-link sidebar-link-disabled">
            <span aria-hidden="true">{link.icon}</span>
            {link.label}
          </span>
        ))}
      </nav>

      <div className="sidebar-footer spots">
        🐞 Squash bugs before they spread — spot risk early.
      </div>
    </aside>
  )
}
