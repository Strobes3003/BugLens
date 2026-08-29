import { useCallback, useMemo, useState } from 'react'
import { ToastContext } from './ToastContext'
import './ui.css'

let idCounter = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismissToast = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const showToast = useCallback(
    (message, { variant = 'default', duration = 4000 } = {}) => {
      const id = ++idCounter
      setToasts((current) => [...current, { id, message, variant }])
      if (duration) {
        setTimeout(() => dismissToast(id), duration)
      }
      return id
    },
    [dismissToast],
  )

  const value = useMemo(() => ({ showToast, dismissToast }), [showToast, dismissToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-viewport" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.variant}`}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
