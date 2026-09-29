import request from '@/utils/request'
import type { PageVO } from '@/types/product'

export interface MemberLevel {
  id: number
  name: string
  pointsThreshold: number
  discount: number
  sortOrder: number
}

export interface Member {
  id: number
  memberNo: string
  name: string
  phone: string
  gender: number
  birthday: string | null
  levelId: number
  levelName: string | null
  points: number
  balance: number
  totalSpent: number
  status: number
  createdAt: string
}

export interface PointsRecord {
  id: number
  memberId: number
  change: number
  type: string
  refType: string | null
  refNo: string | null
  remark: string | null
  createdAt: string
}

export interface ConsumptionRecord {
  id: number
  memberId: number
  salesOrderId: number
  orderNo: string
  amount: number
  pointsEarned: number
  createdAt: string
}

export function pageMembers(query: {
  page: number
  size: number
  keyword?: string
  levelId?: number
  status?: number
}) {
  return request.get<PageVO<Member>>('/members', { params: query })
}

export function createMember(data: {
  name: string
  phone: string
  gender?: number
  birthday?: string | null
}) {
  return request.post<Member>('/members', data)
}

export function updateMember(
  id: number,
  data: { name: string; phone: string; gender?: number; birthday?: string | null }
) {
  return request.put<Member>(`/members/${id}`, data)
}

export function updateMemberStatus(id: number, status: number) {
  return request.put<void>(`/members/${id}/status`, null, { params: { status } })
}

export function adjustMemberPoints(id: number, data: { change: number; reason: string }) {
  return request.post<void>(`/members/${id}/points`, data)
}

export function memberPointsRecords(id: number) {
  return request.get<PointsRecord[]>(`/members/${id}/points-records`)
}

export function memberConsumptions(id: number) {
  return request.get<ConsumptionRecord[]>(`/members/${id}/consumptions`)
}

export function memberLevels() {
  return request.get<MemberLevel[]>('/member-levels')
}
