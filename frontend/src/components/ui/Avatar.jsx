import './ui.css'

function getInitials(name = '') {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

export default function Avatar({ name = '', src, size = 32 }) {
  const style = { width: size, height: size, fontSize: size * 0.4 }
  return (
    <span className="avatar" style={style} title={name}>
      {src ? <img src={src} alt={name} /> : getInitials(name) || '?'}
    </span>
  )
}
