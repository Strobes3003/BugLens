import { NavLink } from 'react-router-dom'
import { ROUTES } from '../../utils/constants'
import './layout.css'

const PRIMARY_LINKS = [
  { to: ROUTES.DASHBOARD, label: 'Dashboard', icon: '🏠' },
  { to: ROUTES.WORKSPACE, label: 'Workspace', icon: '🧭' },
  { to: ROUTES.PROJECTS, label: 'Projects', icon: '📁' },
  { to: ROUTES.ISSUES, label: 'Issues', icon: '🐞' },
  { to: ROUTES.DEPENDENCIES, label: 'Dependencies', icon: '🔗' },
  { to: ROUTES.FIX_NEXT, label: 'Fix Next', icon: '🎯' },
  { to: ROUTES.RELEASE_RISK, label: 'Release Risk', icon: '📊' },
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

      <div className="sidebar-footer spots">
        🐞 Squash bugs before they spread — spot risk early.
      </div>
    </aside>
  )
}
