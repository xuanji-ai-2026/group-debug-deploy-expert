const TOKEN_KEY = 'admin_token'
const REFRESH_TOKEN_KEY = 'admin_refresh_token'
const ADMIN_INFO_KEY = 'admin_info'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token) {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function removeRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getAdminInfo() {
  const info = localStorage.getItem(ADMIN_INFO_KEY)
  try {
    return info ? JSON.parse(info) : null
  } catch {
    return null
  }
}

export function setAdminInfo(info) {
  localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(info))
}

export function removeAdminInfo() {
  localStorage.removeItem(ADMIN_INFO_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function getAdminRole() {
  const info = getAdminInfo()
  return info?.role || null
}

export function isSuperAdmin() {
  return getAdminRole() === 'super_admin'
}

export function logout() {
  removeToken()
  removeRefreshToken()
  removeAdminInfo()
  window.location.href = '/#/login'
}

export default {
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
  removeRefreshToken,
  getAdminInfo,
  setAdminInfo,
  removeAdminInfo,
  isLoggedIn,
  getAdminRole,
  isSuperAdmin,
  logout
}
