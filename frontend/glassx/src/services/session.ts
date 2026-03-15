import type { AdminActionKey, UserInfo } from '@/types'

const ADMIN_TOKEN_KEY = 'admin_token'
const IS_ADMIN_KEY = 'is_admin'
const ADMIN_ACTIONS_KEY = 'admin_actions'
const USER_INFO_KEY = 'user_info'
const TOKEN_EXPIRY_KEY = 'token_expiry'
const LOGIN_PATH = '/login'

// Token 默认有效期：7天（毫秒）
const DEFAULT_TOKEN_LIFETIME_MS = 7 * 24 * 60 * 60 * 1000

const canUseBrowser = (): boolean => typeof window !== 'undefined'

const normalizeAdminActions = (adminActions?: AdminActionKey[]): AdminActionKey[] => {
  if (!adminActions?.length) return []
  return Array.from(new Set(adminActions.filter(Boolean)))
}

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
    localStorage.removeItem(ADMIN_ACTIONS_KEY)
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
    if (this.getAdminActions().length > 0) {
      return true
    }
    return localStorage.getItem(IS_ADMIN_KEY) === 'true'
  },

  setAdminStatus(isAdmin: boolean): void {
    if (!canUseBrowser()) return
    localStorage.setItem(IS_ADMIN_KEY, String(isAdmin))
  },

  getAdminActions(): AdminActionKey[] {
    if (!canUseBrowser()) return []
    const adminActionsStr = localStorage.getItem(ADMIN_ACTIONS_KEY)
    if (!adminActionsStr) return []
    try {
      const parsed = JSON.parse(adminActionsStr)
      return Array.isArray(parsed) ? normalizeAdminActions(parsed as AdminActionKey[]) : []
    } catch {
      return []
    }
  },

  setAdminActions(adminActions: AdminActionKey[]): void {
    if (!canUseBrowser()) return
    const normalizedAdminActions = normalizeAdminActions(adminActions)
    localStorage.setItem(ADMIN_ACTIONS_KEY, JSON.stringify(normalizedAdminActions))
    this.setAdminStatus(normalizedAdminActions.length > 0)
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
    const adminActions = normalizeAdminActions(userInfo.adminActions)
    const normalizedUserInfo: UserInfo = {
      ...userInfo,
      adminActions,
      isAdmin: adminActions.length > 0 || userInfo.isAdmin === true,
    }
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(normalizedUserInfo))
    this.setAdminActions(adminActions)
    if (normalizedUserInfo.isAdmin !== undefined) {
      this.setAdminStatus(normalizedUserInfo.isAdmin)
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
