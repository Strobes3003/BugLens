import LoginForm from '../features/auth/components/LoginForm'
import '../features/auth/components/auth.css'

export default function LoginPage() {
  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-brand">
          BugLens<span className="auth-brand-dot">.</span>
        </div>
        <LoginForm />
      </div>
    </div>
  )
}
