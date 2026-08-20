import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getNotice } from '@/api/config'

/**
 * 店铺全局状态（Pinia store）。
 * 目前只存"店铺公告"：它来自 Nacos 配置中心（/api/config/notice），
 * 演示"改配置不重启、动态刷新"的特性，所以放在全局 store 供顶栏常驻展示。
 */
export const useShopStore = defineStore('shop', () => {
  /** 店铺公告文本 */
  const notice = ref('')

  /** 拉取公告（App 挂载时调用一次） */
  async function fetchNotice() {
    notice.value = await getNotice()
  }

  return { notice, fetchNotice }
})
