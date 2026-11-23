import { get, post } from './request'
import type { Order, OrderCreateDTO } from '@/types'

/**
 * 订单服务接口封装（对应后端 OrderController）。
 */

/**
 * 生成下单幂等键。
 * 一个"下单意图"（选定的商品 + 数量）对应一个键：连点、超时重发都带同一个键，后端只落一单；
 * 下单成功或改了商品/数量后再换新键。
 * crypto.randomUUID 只在安全上下文（https / localhost）可用，局域网 IP 访问时降级为随机串。
 */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`
}

/** 创建订单（走 Seata 分布式事务）：POST /api/order/create，返回订单 ID */
export function createOrder(dto: OrderCreateDTO) {
  return post<string>('/order/create', dto)
}

/** 按 ID 查询订单：GET /api/order/{id} */
export function getOrder(id: string) {
  return get<Order>(`/order/${id}`)
}

/** 订单列表：GET /api/order/list */
export function listOrders() {
  return get<Order[]>('/order/list')
}
