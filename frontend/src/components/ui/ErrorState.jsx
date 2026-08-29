import './ui.css'
import Button from './Button'

export default function ErrorState({
  title = 'Something went wrong',
  message = 'Please try again in a moment.',
  onRetry,
}) {
  return (
    <div className="state-block">
      <div className="state-icon" aria-hidden="true">
        ⚠️
      </div>
      <h3>{title}</h3>
      <p>{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
