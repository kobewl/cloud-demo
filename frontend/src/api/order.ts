import { get, post } from './request'
import type { Order, OrderCreateDTO } from '@/types'

/**
 * 订单服务接口封装（对应后端 OrderController）。
 */

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
