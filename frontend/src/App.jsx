import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './features/auth/components/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import WorkspacePage from './pages/WorkspacePage'
import ProjectsPage from './pages/ProjectsPage'
import ProjectSettingsPage from './pages/ProjectSettingsPage'
import IssuesPage from './pages/IssuesPage'
import IssueDetailPage from './pages/IssueDetailPage'
import DependencyGraphPage from './pages/DependencyGraphPage'
import FixNextPage from './pages/FixNextPage'
import ReleaseRiskPage from './pages/ReleaseRiskPage'
import { ROUTES } from './utils/constants'

function App() {
  return (
    <Routes>
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.REGISTER} element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
          <Route path={ROUTES.WORKSPACE} element={<WorkspacePage />} />
          <Route path={ROUTES.PROJECTS} element={<ProjectsPage />} />
          <Route
            path={ROUTES.PROJECT_SETTINGS}
            element={<ProjectSettingsPage />}
          />

          <Route path={ROUTES.ISSUES} element={<IssuesPage />} />
          <Route path={ROUTES.ISSUE_DETAIL} element={<IssueDetailPage />} />
          <Route path={ROUTES.DEPENDENCIES} element={<DependencyGraphPage />} />
          <Route path={ROUTES.FIX_NEXT} element={<FixNextPage />} />
          <Route path={ROUTES.RELEASE_RISK} element={<ReleaseRiskPage />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
      <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
    </Routes>
  )
}

export default App
