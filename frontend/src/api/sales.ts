import request from '@/utils/request'
import type { PageVO } from '@/types/product'

export interface SalesOrder {
  id: number
  orderNo: string
  memberId: number | null
  memberName: string | null
  status: number
  statusLabel: string
  totalAmount: number
  discountAmount: number
  payableAmount: number
  payMethod: string
  pointsEarned: number
  remark: string | null
  createdAt: string
}

export interface SalesOrderItem {
  id: number
  productId: number
  productName: string
  sku: string
  unit: string
  quantity: number
  unitPrice: number
  discountAmount: number
  amount: number
  promotionId: number | null
  returnedQuantity: number
}

export const PAY_METHODS: Record<string, string> = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CARD: '银行卡'
}

export function checkout(data: {
  memberId?: number
  couponCode?: string
  payMethod: string
  remark?: string
  items: { productId: number; quantity: number }[]
}) {
  return request.post<SalesOrder>('/sales-orders', data)
}

export function pageSalesOrders(query: {
  page: number
  size: number
  keyword?: string
  memberId?: number
  status?: number
}) {
  return request.get<PageVO<SalesOrder>>('/sales-orders', { params: query })
}

export function getSalesOrder(id: number) {
  return request.get<{ order: SalesOrder; items: SalesOrderItem[] }>(`/sales-orders/${id}`)
}

export function createSalesReturn(data: {
  orderId: number
  reason?: string
  items: { orderItemId: number; quantity: number }[]
}) {
  return request.post<{ returnNo: string; refundAmount: number }>('/sales-orders/returns', data)
}
