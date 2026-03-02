import type { UserInfo } from '@/types'

const ADMIN_TOKEN_KEY = 'admin_token'
const IS_ADMIN_KEY = 'is_admin'
const USER_INFO_KEY = 'user_info'
const TOKEN_EXPIRY_KEY = 'token_expiry'
const LOGIN_PATH = '/login'

// Token 默认有效期：7天（毫秒）
const DEFAULT_TOKEN_LIFETIME_MS = 7 * 24 * 60 * 60 * 1000

const canUseBrowser = (): boolean => typeof window !== 'undefined'

const getCurrentLocationForRedirect = (): string => {
  if (!canUseBrowser()) return '/'
  const { pathname, search, hash } = window.location
  return `${pathname}${search}${hash}`
}

const redirectToLogin = (redirect?: string): void => {
  if (!canUseBrowser()) return

  const loginUrl = new URL(LOGIN_PATH, window.location.origin)
  const target = redirect ?? getCurrentLocationForRedirect()

  if (target && target !== LOGIN_PATH && !target.startsWith(`${LOGIN_PATH}?`)) {
    loginUrl.searchParams.set('redirect', target)
  }

  const destination = `${loginUrl.pathname}${loginUrl.search}`
  const current = `${window.location.pathname}${window.location.search}`

  if (current !== destination) {
    window.location.assign(destination)
  }
}

export const sessionService = {
  getToken(): string | null {
    if (!canUseBrowser()) return null
    return localStorage.getItem(ADMIN_TOKEN_KEY)
  },

  setToken(token: string, expiresInMs?: number): void {
    if (!canUseBrowser()) return
    localStorage.setItem(ADMIN_TOKEN_KEY, token)
    // 设置过期时间
    const expiryTime = Date.now() + (expiresInMs ?? DEFAULT_TOKEN_LIFETIME_MS)
    localStorage.setItem(TOKEN_EXPIRY_KEY, String(expiryTime))
  },

  // 检查 token 是否过期
  isTokenExpired(): boolean {
    if (!canUseBrowser()) return true
    const expiryStr = localStorage.getItem(TOKEN_EXPIRY_KEY)
    if (!expiryStr) return true // 没有过期时间记录，视为过期
    const expiryTime = parseInt(expiryStr, 10)
    if (isNaN(expiryTime)) return true
    return Date.now() > expiryTime
  },

  clearToken(): void {
    if (!canUseBrowser()) return
    localStorage.removeItem(ADMIN_TOKEN_KEY)
    localStorage.removeItem(IS_ADMIN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
    localStorage.removeItem(TOKEN_EXPIRY_KEY)
  },

  isAuthenticated(): boolean {
    const token = this.getToken()
    if (token === null) return false
    // 检查 token 是否过期
    if (this.isTokenExpired()) {
      this.clearToken()
      return false
    }
    return true
  },

  // Admin status management
  isAdmin(): boolean {
    if (!canUseBrowser()) return false
    return localStorage.getItem(IS_ADMIN_KEY) === 'true'
  },

  setAdminStatus(isAdmin: boolean): void {
    if (!canUseBrowser()) return
    localStorage.setItem(IS_ADMIN_KEY, String(isAdmin))
  },

  // User info management
  getUserInfo(): UserInfo | null {
    if (!canUseBrowser()) return null
    const userInfoStr = localStorage.getItem(USER_INFO_KEY)
    if (!userInfoStr) return null
    try {
      return JSON.parse(userInfoStr)
    } catch {
      return null
    }
  },

  setUserInfo(userInfo: UserInfo): void {
    if (!canUseBrowser()) return
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
    if (userInfo.isAdmin !== undefined) {
      this.setAdminStatus(userInfo.isAdmin)
    }
  },

  getPostLoginRedirect(): string | null {
    if (!canUseBrowser()) return null
    const redirect = new URLSearchParams(window.location.search).get('redirect')
    return redirect && redirect.startsWith('/') ? redirect : null
  },

  redirectToLogin,

  handleUnauthorized(redirect?: string): void {
    this.clearToken()
    redirectToLogin(redirect)
  },
}
