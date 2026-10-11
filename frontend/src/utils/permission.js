export function hasPermission(permissions, required) {
  if (!required) return true
  if (!Array.isArray(permissions)) return false
  return permissions.includes('*:*:*') || permissions.includes(required)
}

export function hasAnyPermission(permissions, requiredList) {
  if (!Array.isArray(requiredList) || requiredList.length === 0) return true
  return requiredList.some(required => hasPermission(permissions, required))
}
