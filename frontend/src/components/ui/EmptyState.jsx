import './ui.css'

export default function EmptyState({
  icon = '🗂️',
  title = 'Nothing here yet',
  description,
  action,
}) {
  return (
    <div className="state-block">
      <div className="state-icon" aria-hidden="true">
        {icon}
      </div>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}
