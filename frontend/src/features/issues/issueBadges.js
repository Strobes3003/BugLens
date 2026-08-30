/**
 * One place deciding how an issue's enums are worded and colored, so the metadata
 * card, the workflow panel and anything added later cannot drift apart.
 *
 * Every badge carries its own text label — color is never the only thing telling
 * you a state apart.
 */

/** OPEN -> "Open", IN_PROGRESS -> "In progress" */
export function humanize(value) {
  if (!value) return ''
  return value
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ')
}

const STATUS_VARIANTS = {
  OPEN: 'default',
  IN_PROGRESS: 'info',
  IN_REVIEW: 'warning',
  RESOLVED: 'success',
  CLOSED: 'default',
}

// Severity and priority share the same four levels and the same escalation.
const LEVEL_VARIANTS = {
  LOW: 'default',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'critical',
}

export function statusVariant(status) {
  return STATUS_VARIANTS[status] ?? 'default'
}

export function levelVariant(level) {
  return LEVEL_VARIANTS[level] ?? 'default'
}
