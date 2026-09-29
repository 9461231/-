import axios from 'axios'
import { ElMessage } from 'element-plus'

// Agent 服务独立于 Spring Boot 后端，单独走 /agent-api 代理
const client = axios.create({ baseURL: '/agent-api', timeout: 120000 })

client.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    ElMessage.error(error?.message || 'Agent 服务不可用，请确认 agent-service 已启动')
    return Promise.reject(error)
  }
)

export interface PendingAction {
  action_id: string
  kind: string
  summary: string
  payload: Record<string, unknown>
}

export interface ChatResponse {
  reply: string
  pending: PendingAction[]
}

export function agentHealth() {
  return client.get<never, { status: string; mode: string; backend: string }>('/api/agent/health')
}

export function agentChat(sessionId: string, message: string) {
  return client.post<never, ChatResponse>('/api/agent/chat', { sessionId, message })
}

export function approveAction(sessionId: string, actionId: string) {
  return client.post<never, { reply: string }>(`/api/agent/actions/${actionId}/approve`, { sessionId })
}

export function cancelAction(sessionId: string, actionId: string) {
  return client.post<never, { reply: string }>(`/api/agent/actions/${actionId}/cancel`, { sessionId })
}
