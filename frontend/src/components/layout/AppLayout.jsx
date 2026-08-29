import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Sidebar from './Sidebar'
import { WorkspaceProvider } from '../../features/workspace/context/WorkspaceProvider'
import './layout.css'

export default function AppLayout() {
  return (
    <WorkspaceProvider>
      <div className="app-shell">
        <Navbar />
        <Sidebar />
        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </WorkspaceProvider>
  )
}
