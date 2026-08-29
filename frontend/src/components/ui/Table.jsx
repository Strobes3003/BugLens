import './ui.css'
import EmptyState from './EmptyState'

export default function Table({
  columns,
  data,
  keyField = 'id',
  emptyMessage = 'Nothing to show yet.',
}) {
  if (!data || data.length === 0) {
    return <EmptyState title={emptyMessage} />
  }

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={row[keyField]}>
              {columns.map((col) => (
                <td key={col.key}>
                  {col.render ? col.render(row) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
