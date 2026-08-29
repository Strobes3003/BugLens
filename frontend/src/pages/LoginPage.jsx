import LoginForm from '../features/auth/components/LoginForm'
import LadybugMark from '../components/common/LadybugMark'
import LadybugScene from '../components/common/LadybugScene'
import '../features/auth/components/auth.css'

export default function LoginPage() {
  return (
    <div className="auth-screen">
      <LadybugScene />
      <div className="auth-overlay">
        <div className="auth-card">
          <div className="auth-brand">
            <LadybugMark size={30} /> BugLens
          </div>
          <LoginForm />
        </div>
      </div>
      <p className="auth-hint">
        🐞 Move your cursor · Click anywhere to make her loop
      </p>
    </div>
  )
}
