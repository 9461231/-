import request from '@/utils/request'
import type { PageVO, Supplier, SupplierProduct, SupplierQuery } from '@/types/supplier'

export function pageSuppliers(query: SupplierQuery) {
  return request.get<PageVO<Supplier>>('/suppliers', { params: query })
}

export function supplierOptions() {
  return request.get<Supplier[]>('/suppliers/options')
}

export function createSupplier(data: Partial<Supplier>) {
  return request.post<Supplier>('/suppliers', data)
}

export function updateSupplier(id: number, data: Partial<Supplier>) {
  return request.put<Supplier>(`/suppliers/${id}`, data)
}

export function deleteSupplier(id: number) {
  return request.delete<void>(`/suppliers/${id}`)
}

export function listSupplierProducts(supplierId: number) {
  return request.get<SupplierProduct[]>(`/suppliers/${supplierId}/products`)
}

export function bindSupplierProduct(
  supplierId: number,
  data: { productId: number; supplyPrice: number; isPrimary?: boolean }
) {
  return request.post<SupplierProduct>(`/suppliers/${supplierId}/products`, data)
}

export function updateSupplyPrice(supplierId: number, productId: number, supplyPrice: number) {
  return request.put<void>(`/suppliers/${supplierId}/products/${productId}`, { supplyPrice })
}

export function unbindSupplierProduct(supplierId: number, productId: number) {
  return request.delete<void>(`/suppliers/${supplierId}/products/${productId}`)
}
