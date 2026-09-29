import request from '@/utils/request'
import type { PageVO } from '@/types/product'

export interface Stocktake {
  id: number
  taskNo: string
  status: number
  statusLabel: string
  remark: string | null
  completedAt: string | null
  createdAt: string
  items: StocktakeItem[]
}

export interface StocktakeItem {
  id: number
  productId: number
  productName: string
  sku: string
  unit: string
  systemQty: number
  actualQty: number | null
  diff: number | null
}

export interface LossRecord {
  id: number
  productId: number
  productName: string
  sku: string
  quantity: number
  lossAmount: number
  reason: string
  createdAt: string
}

export function pageStocktakes(query: { page: number; size: number }) {
  return request.get<PageVO<Stocktake>>('/stocktakes', { params: query })
}

export function createStocktake(data: { productIds: number[]; remark?: string }) {
  return request.post<Stocktake>('/stocktakes', data)
}

export function recordStocktakeItem(stocktakeId: number, itemId: number, actualQty: number) {
  return request.put<void>(`/stocktakes/${stocktakeId}/items/${itemId}`, { actualQty })
}

export function completeStocktake(id: number) {
  return request.post<Stocktake>(`/stocktakes/${id}/complete`)
}

export function cancelStocktake(id: number) {
  return request.post<void>(`/stocktakes/${id}/cancel`)
}

export function pageLossRecords(query: { page: number; size: number }) {
  return request.get<PageVO<LossRecord>>('/loss-records', { params: query })
}

export function createLossRecord(data: { productId: number; quantity: number; reason: string; unitCost?: number }) {
  return request.post<LossRecord>('/loss-records', data)
}
