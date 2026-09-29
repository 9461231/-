export interface ProductCategory {
  id: number
  name: string
  sortOrder: number
  status: number
  productCount: number
  createdAt: string
  updatedAt: string
}

export interface Product {
  id: number
  categoryId: number
  categoryName: string
  sku: string
  barcode: string | null
  name: string
  brand: string | null
  spec: string | null
  unit: string
  purchasePrice: number
  salePrice: number
  memberPrice: number | null
  minSalePrice: number | null
  minStock: number
  maxStock: number | null
  status: number
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface ProductQuery {
  page: number
  size: number
  keyword?: string
  categoryId?: number
  status?: number
}

export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface CategoryRequest {
  name: string
  sortOrder?: number
  status?: number
}

export interface ProductRequest {
  categoryId: number
  sku: string
  barcode?: string | null
  name: string
  brand?: string | null
  spec?: string | null
  unit: string
  purchasePrice: number
  salePrice: number
  memberPrice?: number | null
  minSalePrice?: number | null
  minStock: number
  maxStock?: number | null
  status?: number
  remark?: string | null
}
