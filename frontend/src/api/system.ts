import request from '@/utils/request'
import request2 from '@/utils/request'

export interface LoginVO {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  roleLabel: string
  storeId: number | null
  storeName: string
}

export function login(username: string, password: string) {
  return request.post<LoginVO>('/auth/login', { username, password })
}

export function logoutApi() {
  return request2.post<void>('/auth/logout')
}

// ============ 员工 / 店家 / 审计（系统管理） ============

export interface UserVO {
  id: number
  username: string
  realName: string
  role: string
  roleLabel: string
  storeId: number | null
  storeName: string
  status: number
  createdAt: string
}

export function pageUsers(query: { page: number; size: number; keyword?: string; role?: string }) {
  return request.get<{ list: UserVO[]; total: number }>('/users', { params: query })
}

export function createUser(data: {
  username: string
  password: string
  realName?: string
  role: string
}) {
  return request.post<UserVO>('/users', data)
}

export function updateUser(id: number, data: { realName?: string; role?: string; status?: number }) {
  return request.put<UserVO>(`/users/${id}`, data)
}

export function resetUserPassword(id: number, password: string) {
  return request.put<void>(`/users/${id}/password`, { password })
}

export function updateUserStatus(id: number, status: number) {
  return request.put<void>(`/users/${id}/status`, null, { params: { status } })
}

export function pageStores(query: { page: number; size: number; keyword?: string }) {
  return request.get<{ list: any[]; total: number }>('/stores', { params: query })
}

export function updateStore(id: number, data: any) {
  return request.put<void>(`/stores/${id}`, data)
}

export function pageAuditLogs(query: {
  page: number
  size: number
  username?: string
  targetType?: string
  source?: string
}) {
  return request.get<{ list: any[]; total: number }>('/audit-logs', { params: query })
}

// ============ 消息中心 ============

export function messageSummary() {
  return request.get<Record<string, number>>('/messages/summary')
}

export function messageInventoryAlerts() {
  return request.get<any[]>('/messages/inventory-alerts')
}

export function messageExpiring() {
  return request.get<{ expired: any[]; urgent: any[]; near: any[] }>('/messages/expiring')
}
