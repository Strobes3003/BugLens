import './ui.css'

export default function Tabs({ tabs, activeKey, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          type="button"
          role="tab"
          aria-selected={tab.key === activeKey}
          className={`tab ${tab.key === activeKey ? 'tab-active' : ''}`}
          onClick={() => onChange(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
