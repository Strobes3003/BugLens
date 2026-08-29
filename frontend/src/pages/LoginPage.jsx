import LoginForm from '../features/auth/components/LoginForm'
import LadybugMark from '../components/common/LadybugMark'
import '../features/auth/components/auth.css'

export default function LoginPage() {
  return (
    <div className="auth-screen spots">
      <div className="auth-card">
        <div className="auth-brand">
          <LadybugMark size={30} /> BugLens
        </div>
        <LoginForm />
      </div>
    </div>
  )
}
