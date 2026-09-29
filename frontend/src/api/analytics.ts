import request from '@/utils/request'

export interface Dashboard {
  todaySales: number
  todayOrders: number
  monthSales: number
  monthOrders: number
  monthPurchase: number
  monthProfit: number
  inventoryValue: number
  alertCount: number
  memberCount: number
  memberSalesRatio: number
  monthLoss: number
}

export function dashboard() {
  return request.get<Dashboard>('/analytics/dashboard')
}

export function salesTrend(days = 14) {
  return request.get<{ date: string; amount: number; orders: number }[]>('/analytics/sales-trend', {
    params: { days }
  })
}

export function topProducts(days = 30, limit = 10) {
  return request.get<{ productId: number; productName: string; sku: string; quantity: number; amount: number }[]>(
    '/analytics/top-products',
    { params: { days, limit } }
  )
}

export function slowProducts(days = 30, limit = 10) {
  return request.get<{ productId: number; productName: string; sku: string; quantity: number; stock: number; coverDays: number | null }[]>(
    '/analytics/slow-products',
    { params: { days, limit } }
  )
}

export function categorySales(days = 30) {
  return request.get<{ category: string; amount: number }[]>('/analytics/category-sales', {
    params: { days }
  })
}

export function lossStats(months = 6) {
  return request.get<{ month: string; amount: number }[]>('/analytics/loss-stats', {
    params: { months }
  })
}

export function purchaseTrend(months = 6) {
  return request.get<{ month: string; amount: number }[]>('/analytics/purchase-trend', {
    params: { months }
  })
}
