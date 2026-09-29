import request from '@/utils/request'
import type { PageVO, Product, ProductCategory } from '@/types/product'
import type { Supplier } from '@/types/supplier'

export interface InventoryVO {
  inventoryId: number | null
  productId: number
  productName: string
  sku: string
  unit: string
  categoryName?: string
  quantity: number
  minStock: number
  maxStock: number | null
  inventoryValue: number | null
  alertType: 'OUT_OF_STOCK' | 'LOW' | 'HIGH' | null
  updatedAt: string | null
}

export interface InventoryAlertVO {
  productId: number
  productName: string
  sku: string
  quantity: number
  minStock: number
  maxStock: number | null
  alertType: 'OUT_OF_STOCK' | 'LOW' | 'HIGH'
  suggestQty: number | null
}

export interface InventoryTransactionVO {
  id: number
  productId: number
  productName: string
  sku: string
  type: string
  changeQty: number
  beforeQty: number
  afterQty: number
  refType: string | null
  refNo: string | null
  remark: string | null
  createdAt: string
}

export interface PurchaseOrder {
  id: number
  orderNo: string
  supplierId: number
  supplierName: string
  status: number
  statusLabel: string
  totalAmount: number
  itemCount: number | null
  remark: string | null
  auditRemark: string | null
  createdAt: string
}

export interface PurchaseOrderItem {
  id: number
  productId: number
  productName: string
  sku: string
  unit: string
  quantity: number
  purchasePrice: number
  amount: number
  receivedQuantity: number
}

export interface PurchaseReturn {
  id: number
  returnNo: string
  supplierId: number
  supplierName: string
  orderId: number | null
  totalAmount: number
  reason: string | null
  createdAt: string
  items: {
    productId: number
    productName: string
    quantity: number
    purchasePrice: number
  }[]
}

export const TX_TYPE_LABELS: Record<string, string> = {
  PURCHASE_IN: '采购入库',
  SALE_OUT: '销售出库',
  SALE_RETURN_IN: '销售退货',
  PURCHASE_RETURN_OUT: '采购退货',
  STOCKTAKE_ADJUST: '盘点调整',
  MANUAL_ADJUST: '手工调整',
  LOSS_OUT: '损耗出库'
}

export const ALERT_LABELS: Record<string, string> = {
  OUT_OF_STOCK: '缺货',
  LOW: '低于最低库存',
  HIGH: '高于最高库存'
}

// ---------------- 库存 ----------------

export function pageInventory(query: {
  page: number
  size: number
  keyword?: string
  categoryId?: number
  alertType?: string
}) {
  return request.get<PageVO<InventoryVO>>('/inventory', { params: query })
}

export function inventoryAlerts() {
  return request.get<InventoryAlertVO[]>('/inventory/alerts')
}

export function pageInventoryTransactions(query: {
  page: number
  size: number
  productId?: number
  type?: string
}) {
  return request.get<PageVO<InventoryTransactionVO>>('/inventory/transactions', { params: query })
}

export function adjustInventory(data: { productId: number; changeQty: number; reason: string }) {
  return request.post<void>('/inventory/adjust', data)
}

// ---------------- 采购订单 ----------------

export function pagePurchaseOrders(query: {
  page: number
  size: number
  keyword?: string
  supplierId?: number
  status?: number
}) {
  return request.get<PageVO<PurchaseOrder>>('/purchase-orders', { params: query })
}

export function getPurchaseOrder(id: number) {
  return request.get<{ order: PurchaseOrder; items: PurchaseOrderItem[] }>(`/purchase-orders/${id}`)
}

export function createPurchaseOrder(data: {
  supplierId: number
  remark?: string
  items: { productId: number; quantity: number; purchasePrice: number }[]
}) {
  return request.post<PurchaseOrder>('/purchase-orders', data)
}

export function updatePurchaseOrder(
  id: number,
  data: {
    supplierId: number
    remark?: string
    items: { productId: number; quantity: number; purchasePrice: number }[]
  }
) {
  return request.put<PurchaseOrder>(`/purchase-orders/${id}`, data)
}

export function submitPurchaseOrder(id: number) {
  return request.post<void>(`/purchase-orders/${id}/submit`)
}

export function auditPurchaseOrder(id: number, approved: boolean, auditRemark?: string) {
  return request.post<void>(`/purchase-orders/${id}/audit`, null, {
    params: { approved, auditRemark }
  })
}

export function cancelPurchaseOrder(id: number) {
  return request.post<void>(`/purchase-orders/${id}/cancel`)
}

export function receiptPurchaseOrder(
  id: number,
  data: {
    items: { orderItemId: number; quantity: number }[]
    remark?: string
    productionDate?: string
    shelfLifeDays?: number
  }
) {
  return request.post<{ receiptNo: string; orderStatus: string }>(`/purchase-orders/${id}/receipts`, data)
}

// ---------------- 采购退货 ----------------

export function pagePurchaseReturns(query: { page: number; size: number; keyword?: string }) {
  return request.get<PageVO<PurchaseReturn>>('/purchase-returns', { params: query })
}

export function createPurchaseReturn(data: {
  supplierId: number
  orderId?: number
  reason?: string
  items: { productId: number; quantity: number; purchasePrice: number }[]
}) {
  return request.post<PurchaseReturn>('/purchase-returns', data)
}

export function purchaseMonthStats() {
  return request.get<{ monthOrderCount: number; monthAmount: number }>('/purchase-returns/stats/month')
}

// 联合查询辅助类型
export type { Product, ProductCategory, Supplier }
