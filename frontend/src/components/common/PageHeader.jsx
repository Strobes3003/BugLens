import './common.css'

export default function PageHeader({ title, subtitle, action }) {
  return (
    <div className="page-header">
      <div className="page-header-title">
        <h1>{title}</h1>
        {subtitle && <p className="page-header-subtitle">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}
