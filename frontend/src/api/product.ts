import { get, post, del } from './request'
import type { Product, ProductDetail } from '@/types'

/**
 * 商品服务接口封装（对应后端 ProductController）。
 * 每个函数对应一个后端接口，返回类型都已明确，调用方拿到的就是现成的数据。
 */

/** 商品列表：GET /api/product/list */
export function listProducts() {
  return get<Product[]>('/product/list')
}

/** 按 ID 查询商品：GET /api/product/{id} */
export function getProduct(id: string) {
  return get<Product>(`/product/${id}`)
}

/** 新增商品：POST /api/product */
export function createProduct(product: { name: string; price: number; description?: string }) {
  return post<void>('/product', product)
}

/** 删除商品：DELETE /api/product/{id} */
export function deleteProduct(id: string) {
  return del<void>(`/product/${id}`)
}

/** 商品详情（含库存，走 Feign）：GET /api/product/{id}/detail */
export function getProductDetail(id: string) {
  return get<ProductDetail>(`/product/${id}/detail`)
}

/** 购买商品（直接扣 1 件库存）：POST /api/product/{id}/buy */
export function buyProduct(id: string) {
  return post<void>(`/product/${id}/buy`)
}
