export default function ProjectPicker({ projects, activeProjectId, onSelect }) {
  if (projects.length <= 1) {
    return null
  }

  return (
    <label className="project-picker">
      <span className="project-picker-label">Project</span>
      <select
        value={activeProjectId ?? ''}
        onChange={(event) => onSelect(event.target.value)}
      >
        {projects.map((project) => (
          <option key={project.id} value={project.id}>
            {project.key} — {project.name}
          </option>
        ))}
      </select>
    </label>
  )
}
