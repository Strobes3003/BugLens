const listeners = new Set()

/**
 * Subscribe to "the server rejected our credentials" notifications.
 * Returns an unsubscribe function.
 */
export function onUnauthorized(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function emitUnauthorized() {
  listeners.forEach((listener) => listener())
}
