/** Display formatting shared across features. */

/** OPEN -> "Open", IN_PROGRESS -> "In progress" */
export function humanize(value) {
  if (!value) return ''
  return String(value)
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ')
}

/**
 * "2026-09-15" -> "15 Sep 2026".
 *
 * The parts are pulled apart by hand rather than handed to `new Date(string)`:
 * that parses a bare date as UTC midnight, so anyone west of Greenwich would be
 * shown the day before the release is actually due.
 */
export function formatDate(isoDate) {
  if (!isoDate) return null

  const [year, month, day] = String(isoDate).split('-').map(Number)
  if (!year || !month || !day) return String(isoDate)

  return new Date(year, month - 1, day).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}
