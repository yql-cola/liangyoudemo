import Cookies from 'js-cookie'

const TokenKey = 'Liangyou-Token'

export function getToken() {
  const token = sessionStorage.getItem(TokenKey)
  const legacyToken = Cookies.get(TokenKey)
  if (legacyToken) {
    // Migrate once without changing the original JWT expiration.
    if (!token) sessionStorage.setItem(TokenKey, legacyToken)
    Cookies.remove(TokenKey)
  }
  return token || legacyToken || null
}

export function setToken(token) {
  Cookies.remove(TokenKey)
  sessionStorage.setItem(TokenKey, token)
}

export function removeToken() {
  sessionStorage.removeItem(TokenKey)
  Cookies.remove(TokenKey)
}
