import { Link, useNavigate } from 'react-router-dom'
import { Avatar, Dropdown } from '../ui'
import { useAuth } from '../../features/auth/hooks/useAuth'
import WorkspaceSelector from '../../features/workspace/components/WorkspaceSelector'
import LadybugMark from '../common/LadybugMark'
import { ROUTES } from '../../utils/constants'
import './layout.css'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate(ROUTES.LOGIN, { replace: true })
  }

  return (
    <header className="navbar">
      <div className="navbar-left">
        <Link to={ROUTES.DASHBOARD} className="navbar-brand">
          <LadybugMark size={22} /> BugLens
        </Link>
        <WorkspaceSelector />
      </div>
      <div className="navbar-right">
        <Dropdown
          align="right"
          trigger={
            <span className="navbar-user-trigger">
              <Avatar name={user?.name || ''} size={30} />
            </span>
          }
          items={[
            { key: 'logout', label: 'Log out', danger: true, onClick: handleLogout },
          ]}
        />
      </div>
    </header>
  )
}
