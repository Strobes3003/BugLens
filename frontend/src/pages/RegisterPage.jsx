import RegisterForm from '../features/auth/components/RegisterForm'
import '../features/auth/components/auth.css'

export default function RegisterPage() {
  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-brand">
          BugLens<span className="auth-brand-dot">.</span>
        </div>
        <RegisterForm />
      </div>
    </div>
  )
}
