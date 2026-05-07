export function hasRole(routeRoles = [], userRoles = []) {
  if (!routeRoles?.length) return true
  return routeRoles.some((role) => userRoles.includes(role))
}

export function hasButtonPermission(required, permissions = []) {
  if (!required) return true
  if (Array.isArray(required)) {
    return required.some((item) => permissions.includes(item))
  }
  return permissions.includes(required)
}
