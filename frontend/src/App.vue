<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useShopStore } from '@/stores/shop'

/**
 * 根组件：整体布局（渐变侧边栏 + 顶栏公告 + 面包屑 + 内容区）。
 * 顶栏常驻展示"店铺公告"，演示 Nacos 配置中心动态刷新。
 */
const route = useRoute()
const shop = useShopStore()

// 当前激活的菜单项：让 /products/:id 这类详情页也高亮"商品列表"菜单
const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

// 顶栏当前时间，营造"控制台"的仪式感
const now = ref('')
let timer: ReturnType<typeof setInterval> | undefined

function tick() {
  const d = new Date()
  now.value = d.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  shop.fetchNotice()
  tick()
  timer = setInterval(tick, 1000)
})
</script>

<template>
  <el-container class="layout">
    <!-- 左侧：渐变侧边栏 + 菜单导航 -->
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-logo">☁</div>
        <div class="brand-text">
          <div class="brand-title">cloud-demo</div>
          <div class="brand-sub">微服务控制台</div>
        </div>
      </div>

      <el-menu
        router
        :default-active="activeMenu"
        class="menu"
        background-color="transparent"
        text-color="#a9b1d0"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/order">
          <el-icon><ShoppingCart /></el-icon>
          <span>下单</span>
        </el-menu-item>
        <el-menu-item index="/stock">
          <el-icon><Box /></el-icon>
          <span>库存配置</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>订单列表</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <div class="dot"></div>
        <span>网关 8080 · Nacos 已连接</span>
      </div>
    </el-aside>

    <el-container class="right">
      <!-- 顶栏：面包屑 + 店铺公告 + 时间 -->
      <el-header class="header">
        <div class="header-left">
          <span class="header-title">{{ route.meta.title ?? '' }}</span>
        </div>
        <div class="header-right">
          <div class="notice">
            <el-icon class="notice-icon"><Bell /></el-icon>
            <span class="notice-text" :title="shop.notice">{{ shop.notice || '暂无公告' }}</span>
            <el-button link type="primary" size="small" @click="shop.fetchNotice()">刷新</el-button>
          </div>
          <el-divider direction="vertical" />
          <span class="clock">{{ now }}</span>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, var(--aside-bg-start) 0%, var(--aside-bg-end) 100%);
  color: #fff;
  overflow: hidden;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 20px;
}
.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.brand-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.brand-sub {
  font-size: 12px;
  color: #7d87b0;
  margin-top: 2px;
}

.menu {
  border-right: none;
  padding: 0 12px;
  flex: 1;
}
.menu :deep(.el-menu-item) {
  height: 48px;
  margin: 4px 0;
  border-radius: 10px;
  font-size: 14px;
}
.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
}
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.45);
}
.menu :deep(.el-icon) {
  margin-right: 4px;
}

.aside-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  font-size: 12px;
  color: #7d87b0;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px #34d399;
}

/* ===== 顶栏 ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #eef0f5;
  padding: 0 24px;
}
.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.notice {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #eef1ff;
  border-radius: 20px;
  font-size: 13px;
}
.notice-icon {
  color: var(--el-color-primary);
}
.notice-text {
  color: #374151;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.clock {
  font-size: 13px;
  color: #9ca3af;
  font-variant-numeric: tabular-nums;
}

/* ===== 内容区 ===== */
.main {
  padding: 24px;
  overflow-y: auto;
}

/* 路由切换淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
