import { get } from './request'

/**
 * 配置中心演示接口（对应后端 ConfigController）。
 */

/** 店铺公告：GET /api/config/notice（值来自 Nacos 配置中心，改配置可动态刷新） */
export function getNotice() {
  return get<string>('/config/notice')
}
