import request from '@/utils/request'
import type { PageVO } from '@/types/product'

export const PROMOTION_TYPES: Record<number, string> = {
  1: '直接折扣',
  2: '满减',
  3: '第二件优惠',
  4: '会员专享价'
}

export interface Promotion {
  id: number
  name: string
  type: number
  typeLabel: string
  status: number
  startTime: string
  endTime: string
  discountRate: number | null
  minAmount: number | null
  reduceAmount: number | null
  secondRate: number | null
  memberPrice: number | null
  remark: string | null
  productIds: number[]
  createdAt: string
}

export interface PromotionRequest {
  name: string
  type: number
  startTime: string
  endTime: string
  discountRate?: number
  minAmount?: number
  reduceAmount?: number
  secondRate?: number
  memberPrice?: number
  remark?: string
  productIds: number[]
}

export function pagePromotions(query: { page: number; size: number; type?: number; status?: number }) {
  return request.get<PageVO<Promotion>>('/promotions', { params: query })
}

export function createPromotion(data: PromotionRequest) {
  return request.post<Promotion>('/promotions', data)
}

export function updatePromotion(id: number, data: PromotionRequest) {
  return request.put<Promotion>(`/promotions/${id}`, data)
}

export function deletePromotion(id: number) {
  return request.delete<void>(`/promotions/${id}`)
}

export function updatePromotionStatus(id: number, status: number) {
  return request.put<void>(`/promotions/${id}/status`, null, { params: { status } })
}
