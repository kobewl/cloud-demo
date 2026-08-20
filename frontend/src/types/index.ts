/**
 * 全局类型定义：和后端实体一一对应。
 *
 * 关于 ID 类型为什么是 string：
 * 后端用雪花算法生成 ID（19 位长整型），远超 JS number 的精确范围（2^53），
 * 若用 number 会在 JSON 解析时丢失精度，导致删除/查询"张冠李戴"。
 * 所以后端把 Long 序列化成字符串，前端 ID 统一用 string。
 */

/** 后端统一返回结构 R<T>：code=0 成功，非 0 失败 */
export interface R<T> {
  code: number
  msg: string
  data: T
}

/** 商品实体（对应 service-product 的 Product） */
export interface Product {
  id: string
  name: string
  price: number
  description?: string
  createdAt?: string
}

/** 库存实体（对应 service-stock 的 Stock） */
export interface Stock {
  id: string
  productId: string
  quantity: number
  updatedAt?: string
}

/** 库存信息 DTO（商品详情接口里返回的库存子对象，字段比 Stock 少） */
export interface StockInfo {
  id: string
  productId: string
  quantity: number
}

/** 商品详情接口返回值：商品 + 库存 */
export interface ProductDetail {
  product: Product
  stock: StockInfo | null
}

/** 订单实体（对应 service-order 的 Order） */
export interface Order {
  id: string
  productId: string
  productName: string
  price: number
  count: number
  totalAmount: number
  status: number
  createdAt?: string
  updatedAt?: string
}

/** 下单请求参数（对应 OrderCreateDTO） */
export interface OrderCreateDTO {
  productId: string
  count: number
}

/** 配库存请求参数 */
export interface StockCreateDTO {
  productId: string
  quantity: number
}
