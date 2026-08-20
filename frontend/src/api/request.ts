import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { R } from '@/types'

/**
 * Axios 统一封装：全站所有 HTTP 请求都从这里走。
 *
 * 后端所有接口都返回 R{ code, msg, data }，这里做两件事：
 * 1. 响应拦截器：把 R 外壳"剥掉"，成功时只返回 data，失败时统一弹错误提示。
 * 2. 下面的 get/post/put/del 包装函数：把返回值类型收敛成业务数据 T，
 *    让调用方（api/*.ts）拿到的直接就是商品/订单对象，不用关心 R。
 */

const http = axios.create({
  baseURL: '/api', // 所有请求走相对路径，由 Vite proxy 转发到网关 8080
  timeout: 10000,
})

// 响应拦截器：解包 R，code !== 0 视为业务失败
http.interceptors.response.use(
  (response) => {
    const r = response.data as R<unknown>
    if (r.code === 0) {
      // 成功：把 data 替换到 response.data，后续 .then(res => res.data) 直接拿到业务数据
      response.data = r.data
      return response
    }
    // 业务失败：弹出后端返回的 msg，并中断 Promise 链
    ElMessage.error(r.msg || '请求失败')
    return Promise.reject(new Error(r.msg || '请求失败'))
  },
  (error) => {
    // 网络错误 / HTTP 错误（如网关限流 429）
    const status = error.response?.status
    if (status === 429) {
      ElMessage.error('请求太频繁，被网关 Sentinel 限流了（429），稍后再试')
    } else if (error.response?.data?.msg) {
      ElMessage.error(error.response.data.msg)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查后端服务是否启动')
    } else {
      ElMessage.error('网络错误，请确认网关（8080）已启动')
    }
    return Promise.reject(error)
  },
)

/** GET 请求：返回解包后的业务数据 T */
export function get<T>(url: string, params?: object): Promise<T> {
  return http.get(url, { params }).then((res) => res.data as T)
}

/** POST 请求：body 用 JSON 提交 */
export function post<T>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config).then((res) => res.data as T)
}

/** PUT 请求 */
export function put<T>(url: string, data?: object): Promise<T> {
  return http.put(url, data).then((res) => res.data as T)
}

/** DELETE 请求 */
export function del<T>(url: string): Promise<T> {
  return http.delete(url).then((res) => res.data as T)
}
