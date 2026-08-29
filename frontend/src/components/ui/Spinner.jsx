import './ui.css'

export default function Spinner({ size = 20, inverted = false }) {
  return (
    <span
      className="spinner"
      style={{
        width: size,
        height: size,
        borderColor: inverted ? 'rgba(255,255,255,0.4)' : undefined,
        borderTopColor: inverted ? '#fff' : undefined,
      }}
      role="status"
      aria-label="Loading"
    />
  )
}
