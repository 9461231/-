import { reactive } from 'vue'

export interface AuthUser {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  roleLabel: string
  storeId: number | null
  storeName: string
}

const TOKEN_KEY = 'sm_token'
const USER_KEY = 'sm_user'

export const auth = reactive<{ user: AuthUser | null }>({
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
})

export function isLoggedIn() {
  return !!auth.user && !!localStorage.getItem(TOKEN_KEY)
}

export function setLogin(user: AuthUser) {
  auth.user = user
  localStorage.setItem(TOKEN_KEY, user.token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function logout() {
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function hasRole(...roles: string[]) {
  if (!auth.user) return false
  if (auth.user.role === 'SUPER_ADMIN') return true
  return roles.includes(auth.user.role)
}

export function isOwnerOrAdmin() {
  return hasRole('STORE_OWNER', 'STORE_MANAGER')
}
