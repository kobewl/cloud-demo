import { get, post } from './request'
import type { Stock, StockCreateDTO } from '@/types'

/**
 * 库存服务接口封装（对应后端 StockController）。
 */

/** 按商品 ID 查询库存：GET /api/stock/{productId} */
export function getStock(productId: string) {
  return get<Stock>(`/stock/${productId}`)
}

/** 新增/初始化库存（给商品配库存）：POST /api/stock */
export function createStock(dto: StockCreateDTO) {
  return post<void>('/stock', dto)
}

/** 扣减库存：POST /api/stock/deduct/{productId}/{count}（前端演示一般用不到，主要走下单） */
export function deductStock(productId: string, count: number) {
  return post<void>(`/stock/deduct/${productId}/${count}`)
}
