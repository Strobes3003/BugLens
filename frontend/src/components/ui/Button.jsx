import { forwardRef } from 'react'
import Spinner from './Spinner'
import './ui.css'

const Button = forwardRef(function Button(
  {
    variant = 'primary',
    size = 'md',
    isLoading = false,
    disabled = false,
    leftIcon = null,
    className = '',
    children,
    ...rest
  },
  ref,
) {
  const classes = [
    'btn',
    `btn-${variant}`,
    size === 'sm' ? 'btn-sm' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      ref={ref}
      className={classes}
      disabled={disabled || isLoading}
      {...rest}
    >
      {isLoading ? (
        <Spinner size={14} inverted={variant === 'primary' || variant === 'danger'} />
      ) : (
        leftIcon
      )}
      {children}
    </button>
  )
})

export default Button
