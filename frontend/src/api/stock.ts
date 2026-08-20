import { get, post } from './request'
import type { Stock, StockCreateDTO } from '@/types'

/**
 * 库存服务接口封装（对应后端 StockController）。
 */

/** 按商品 ID 查询库存：GET /api/stock/{productId}（silent=true 时未配库存不弹全局提示） */
export function getStock(productId: string, silent = false) {
  return get<Stock>(`/stock/${productId}`, undefined, silent)
}

/** 新增/初始化库存（给商品配库存）：POST /api/stock */
export function createStock(dto: StockCreateDTO) {
  return post<void>('/stock', dto)
}

/** 扣减库存：POST /api/stock/deduct/{productId}/{count}（前端演示一般用不到，主要走下单） */
export function deductStock(productId: string, count: number) {
  return post<void>(`/stock/deduct/${productId}/${count}`)
}

/** 增加库存（补货）：POST /api/stock/add/{productId}/{count} */
export function addStock(productId: string, count: number) {
  return post<void>(`/stock/add/${productId}/${count}`)
}
