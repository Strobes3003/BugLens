import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { Button, Input } from '../../../components/ui'
import { ROUTES } from '../../../utils/constants'
import './auth.css'

export default function RegisterForm() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
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
      await register(form)
      navigate(ROUTES.DASHBOARD, { replace: true })
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
        label="Full name"
        type="text"
        name="name"
        autoComplete="name"
        value={form.name}
        onChange={handleChange}
        required
      />
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
        autoComplete="new-password"
        minLength={8}
        value={form.password}
        onChange={handleChange}
        required
      />
      <Button type="submit" isLoading={isSubmitting}>
        Create account
      </Button>
      <p className="auth-footer">
        Already have an account? <Link to={ROUTES.LOGIN}>Sign in</Link>
      </p>
    </form>
  )
}
