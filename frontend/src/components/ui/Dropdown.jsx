import { useEffect, useRef, useState } from 'react'
import './ui.css'

export default function Dropdown({ trigger, items, align = 'left' }) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef(null)

  useEffect(() => {
    if (!isOpen) return undefined
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [isOpen])

  return (
    <div className="dropdown" ref={containerRef}>
      <span onClick={() => setIsOpen((open) => !open)}>{trigger}</span>
      {isOpen && (
        <div className={`dropdown-menu dropdown-menu-${align}`} role="menu">
          {items.map((item) => (
            <button
              key={item.key}
              type="button"
              role="menuitem"
              className={`dropdown-item ${item.danger ? 'dropdown-item-danger' : ''}`}
              onClick={() => {
                setIsOpen(false)
                item.onClick?.()
              }}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
