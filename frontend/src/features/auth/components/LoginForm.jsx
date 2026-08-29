import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { Button, Input } from '../../../components/ui'
import { ROUTES } from '../../../utils/constants'
import './auth.css'

export default function LoginForm() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await login(form)
      const redirectTo = location.state?.from?.pathname || ROUTES.DASHBOARD
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      {error && <div className="form-error">{error}</div>}
      <Input
        label="Email"
        type="email"
        name="email"
        autoComplete="email"
        value={form.email}
        onChange={handleChange}
        required
      />
      <Input
        label="Password"
        type="password"
        name="password"
        autoComplete="current-password"
        value={form.password}
        onChange={handleChange}
        required
      />
      <Button type="submit" isLoading={isSubmitting}>
        Sign in
      </Button>
      <p className="auth-footer">
        Don&apos;t have an account? <Link to={ROUTES.REGISTER}>Sign up</Link>
      </p>
    </form>
  )
}
