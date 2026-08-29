export function getStoredValue(key) {
  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

export function setStoredValue(key, value) {
  try {
    if (value === null || value === undefined) {
      window.localStorage.removeItem(key)
    } else {
      window.localStorage.setItem(key, value)
    }
  } catch {
    // localStorage unavailable (private mode, disabled cookies, etc.) — ignore
  }
}
