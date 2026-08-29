import './ui.css'

export default function Card({ children, padded = true, className = '', ...rest }) {
  const classes = ['card', padded ? 'card-padded' : '', className]
    .filter(Boolean)
    .join(' ')
  return (
    <div className={classes} {...rest}>
      {children}
    </div>
  )
}
