import type { PageVO } from './product'

export interface Supplier {
  id: number
  name: string
  contactPerson: string | null
  phone: string | null
  email: string | null
  address: string | null
  status: number
  remark: string | null
  productCount: number
  createdAt: string
  updatedAt: string
}

export interface SupplierProduct {
  productId: number
  productName: string
  sku: string
  unit: string
  supplyPrice: number
  purchasePrice: number
  isPrimary: boolean
  boundAt: string
}

export type SupplierQuery = {
  page: number
  size: number
  keyword?: string
  status?: number
}

export type { PageVO }
