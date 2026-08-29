import { forwardRef, useId } from 'react'
import './ui.css'

const Input = forwardRef(function Input(
  { label, error, hint, id, className = '', ...rest },
  ref,
) {
  const generatedId = useId()
  const inputId = id || generatedId

  return (
    <div className={`field ${className}`}>
      {label && (
        <label className="field-label" htmlFor={inputId}>
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        className={`field-input ${error ? 'has-error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : undefined}
        {...rest}
      />
      {error ? (
        <span className="field-error" id={`${inputId}-error`}>
          {error}
        </span>
      ) : hint ? (
        <span className="field-hint">{hint}</span>
      ) : null}
    </div>
  )
})

export default Input
