export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const AUTH_TOKEN_KEY = 'buglens_token'
export const ACTIVE_WORKSPACE_KEY = 'buglens_active_workspace'
export const ACTIVE_PROJECT_KEY = 'buglens_active_project'

export const ROUTES = {
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  WORKSPACE: '/workspace',
  PROJECTS: '/projects',
  PROJECT_SETTINGS: '/projects/:projectId/settings',
  ISSUES: '/issues',
  ISSUE_DETAIL: '/issues/:issueId',
  DEPENDENCIES: '/dependencies',
  FIX_NEXT: '/fix-next',
  RELEASE_RISK: '/release-risk',
}

export const WORKSPACE_ROLES = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER']
