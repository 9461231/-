import request from '@/utils/request'
import type {
  CategoryRequest,
  PageVO,
  Product,
  ProductCategory,
  ProductQuery,
  ProductRequest
} from '@/types/product'

// ---------------- 商品分类 ----------------

export function listCategories() {
  return request.get<ProductCategory[]>('/product-categories')
}

export function createCategory(data: CategoryRequest) {
  return request.post<ProductCategory>('/product-categories', data)
}

export function updateCategory(id: number, data: CategoryRequest) {
  return request.put<ProductCategory>(`/product-categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete<void>(`/product-categories/${id}`)
}

// ---------------- 商品 ----------------

export function pageProducts(query: ProductQuery) {
  return request.get<PageVO<Product>>('/products', { params: query })
}

export function getProduct(id: number) {
  return request.get<Product>(`/products/${id}`)
}

export function createProduct(data: ProductRequest) {
  return request.post<Product>('/products', data)
}

export function updateProduct(id: number, data: ProductRequest) {
  return request.put<Product>(`/products/${id}`, data)
}

export function deleteProduct(id: number) {
  return request.delete<void>(`/products/${id}`)
}
