import request from '@/utils/request'
import type { Dashboard as _D } from './analytics'

// ============ 批次与临期 ============

export interface BatchVO {
  id: number
  batchNo: string
  productId: number
  productName: string
  sku: string
  unit: string
  supplierName: string | null
  quantity: number
  productionDate: string | null
  shelfLifeDays: number | null
  expireDate: string | null
  daysToExpire: number
  expiryStatus: 'EXPIRED' | 'URGENT' | 'NEAR' | 'OK'
  purchasePrice: number | null
  remark: string | null
  createdAt: string
}

export function listBatches(query: { productId?: number; status?: string }) {
  return request.get<BatchVO[]>('/batches', { params: query })
}

export function createBatch(data: {
  productId: number
  supplierId?: number
  quantity: number
  productionDate?: string
  shelfLifeDays?: number
  expireDate?: string
  purchasePrice?: number
  remark?: string
}) {
  return request.post<BatchVO>('/batches', data)
}

export function disposeBatch(id: number, data: { quantity: number; reason: string }) {
  return request.post<void>(`/batches/${id}/dispose`, data)
}

// ============ 采购结算 ============

export function payOrder(id: number, data: { amount: number; method?: string; remark?: string }) {
  return request.post<any>(`/purchase-orders/${id}/payments`, data)
}

export function listPayments(id: number) {
  return request.get<any[]>(`/purchase-orders/${id}/payments`)
}

export function supplierStats(id: number) {
  return request.get<any>(`/suppliers/${id}/stats`)
}

export function compareSuppliers(productId: number) {
  return request.get<any[]>(`/suppliers/compare`, { params: { productId } })
}

// ============ 交班与日结 ============

export interface CashShift {
  id: number
  cashierName: string
  startTime: string
  endTime: string | null
  status: number
  orderCount: number
  totalAmount: number
  cashAmount: number
  wechatAmount: number
  alipayAmount: number
  cardAmount: number
  balanceAmount: number
  refundAmount: number
  discountAmount: number
  remark: string | null
}

export function openShift(remark?: string) {
  return request.post<CashShift>('/cash-shifts/open', { remark })
}

export function currentShift() {
  return request.get<CashShift | null>('/cash-shifts/current')
}

export function closeShift(id: number, remark?: string) {
  return request.post<CashShift>(`/cash-shifts/${id}/close`, { remark })
}

export function shiftHistory() {
  return request.get<CashShift[]>('/cash-shifts')
}

export function dailySettlement(date?: string) {
  return request.get<{
    date: string
    orderCount: number
    totalAmount: number
    discountAmount: number
    byMethod: Record<string, number>
  }>('/cash-shifts/daily-settlement', { params: { date } })
}

// ============ 优惠券 ============

export interface Coupon {
  id: number
  name: string
  minAmount: number
  reduceAmount: number
  validUntil: string
  totalCount: number
  issuedCount: number
  status: number
  createdAt: string
}

export function listCoupons() {
  return request.get<Coupon[]>('/coupons')
}

export function createCoupon(data: {
  name: string
  minAmount: number
  reduceAmount: number
  validUntil: string
  totalCount: number
}) {
  return request.post<Coupon>('/coupons', data)
}

export function updateCouponStatus(id: number, status: number) {
  return request.put<void>(`/coupons/${id}/status`, null, { params: { status } })
}

export function issueCoupon(id: number, memberId: number) {
  return request.post<any>(`/coupons/${id}/issue`, null, { params: { memberId } })
}

export function memberCoupons(memberId: number) {
  return request.get<any[]>(`/coupons/member/${memberId}`)
}

// ============ 驾驶舱与分析扩展 ============

export function cockpit() {
  return request.get<{
    todaySales: number
    todayOrders: number
    todayProfit: number
    inventoryAlerts: number
    expiringCount: number
    pendingReceiptOrders: number
    newMembersToday: number
  }>('/analytics/cockpit')
}

export function memberAnalysis(days = 30) {
  return request.get<{
    memberCount: number
    memberOrderCount: number
    memberOrderRatio: number
    memberAvgTicket: number
    activeMembers: number
    repurchaseRate: number
  }>('/analytics/member-analysis', { params: { days } })
}

export function inventoryAnalysis() {
  return request.get<{ inventoryValue: number; cogs30d: number; turnoverDays: number | null }>(
    '/analytics/inventory-analysis'
  )
}

export function promotionAnalysis(days = 30) {
  return request.get<{ promotion: string; orderCount: number; amount: number; discountCost: number }[]>(
    '/analytics/promotion-analysis',
    { params: { days } }
  )
}

export function lossReasons(days = 30) {
  return request.get<{ reason: string; count: number; amount: number }[]>(
    '/analytics/loss-reasons',
    { params: { days } }
  )
}

// ============ 会员充值 ============

export function rechargeBalance(id: number, amount: number, remark?: string) {
  return request.post<any>(`/members/${id}/balance`, { amount, remark })
}

// ============ 价格变更记录 ============

export function priceChanges(productId: number) {
  return request.get<any[]>(`/products/${productId}/price-changes`)
}

export function productExportUrl() {
  return '/api/products/export'
}

export function productTemplateUrl() {
  return '/api/products/template'
}
