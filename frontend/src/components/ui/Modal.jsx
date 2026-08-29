import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import './ui.css'

export default function Modal({ isOpen, onClose, title, children, footer }) {
  useEffect(() => {
    if (!isOpen) return undefined
    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose?.()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div
      className="modal-overlay"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose?.()
      }}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}
