<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  Goods,
  ShoppingCart,
  Box,
  List,
  Cpu,
  Bell,
  Refresh,
  Connection,
  FullScreen,
  Link,
  UserFilled,
  Fold,
  Expand,
  Monitor,
} from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'
import NacosNoticeDialog from '@/components/NacosNoticeDialog.vue'

const route = useRoute()
const router = useRouter()
const shop = useShopStore()

const isCollapse = ref(false)
const noticeDialogVisible = ref(false)

// 当前激活的菜单项
const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

// 顶栏当前时间
const now = ref('')
let timer: ReturnType<typeof setInterval> | undefined

function tick() {
  const d = new Date()
  now.value = d.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function openExternal(url: string) {
  window.open(url, '_blank')
}

// 切换全屏
function toggleFullScreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen().catch(() => {})
    }
  }
}

onMounted(() => {
  shop.fetchNotice()
  tick()
  timer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <el-container class="app-layout">
    <!-- 左侧：渐变科技侧边栏 -->
    <el-aside :width="isCollapse ? '72px' : '240px'" class="aside">
      <!-- 品牌 Logo 区域 -->
      <div class="brand" @click="router.push('/dashboard')">
        <div class="brand-logo">
          <span class="logo-icon">☁</span>
        </div>
        <div v-show="!isCollapse" class="brand-text">
          <div class="brand-title gradient-text-primary">cloud-demo</div>
          <div class="brand-sub">微服务治理控制台</div>
        </div>
      </div>

      <!-- 微服务基础设施健康状态条 -->
      <div v-show="!isCollapse" class="service-status-bar">
        <div class="status-indicator">
          <span class="pulse-dot pulse-dot-success"></span>
          <span class="status-text">Gateway 8080 · 就绪</span>
        </div>
        <el-tag size="small" type="success" effect="dark" round>Nacos 8848</el-tag>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        router
        :default-active="activeMenu"
        :collapse="isCollapse"
        class="menu"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title><span>仪表盘概览</span></template>
        </el-menu-item>

        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <template #title><span>商品资产管理</span></template>
        </el-menu-item>

        <el-menu-item index="/order">
          <el-icon><ShoppingCart /></el-icon>
          <template #title><span>极速下单工作台</span></template>
        </el-menu-item>

        <el-menu-item index="/stock">
          <el-icon><Box /></el-icon>
          <template #title><span>智能库存管控</span></template>
        </el-menu-item>

        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <template #title><span>订单与事务中心</span></template>
        </el-menu-item>

        <el-menu-item index="/governance">
          <el-icon><Cpu /></el-icon>
          <template #title><span>微服务治理与拓扑</span></template>
        </el-menu-item>
      </el-menu>

      <!-- 侧边栏底部管理员卡片 -->
      <div class="aside-footer">
        <div class="admin-profile" v-show="!isCollapse">
          <div class="admin-avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="admin-info">
            <div class="admin-name">Spring Cloud</div>
            <div class="admin-role">Alibaba 2023.0</div>
          </div>
        </div>
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </div>
      </div>
    </el-aside>

    <el-container class="right-container">
      <!-- 顶栏：面包屑 + Nacos 动态公告 + 基础设施快捷菜单 + 时钟 -->
      <el-header class="header">
        <div class="header-left">
          <div class="page-title-wrap">
            <span class="header-page-title">{{ route.meta.title ?? '控制台' }}</span>
          </div>
        </div>

        <div class="header-right">
          <!-- Nacos 动态配置公告条 -->
          <div class="notice-pill" @click="noticeDialogVisible = true">
            <div class="notice-badge">
              <el-icon><Bell /></el-icon>
            </div>
            <span class="notice-text" :title="shop.notice">
              {{ shop.notice || 'Nacos 动态配置中心已连接' }}
            </span>
            <el-button link type="primary" size="small" class="notice-edit-btn">
              配置管理
            </el-button>
          </div>

          <el-divider direction="vertical" />

          <!-- 基础设施快捷直达下拉菜单 -->
          <el-dropdown trigger="click">
            <el-button plain size="small" class="infra-btn">
              <el-icon><Link /></el-icon> 基础设施 <el-icon class="el-icon--right"><Connection /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openExternal('http://localhost:18080')">
                  🌐 Nacos 控制台 (:18080)
                </el-dropdown-item>
                <el-dropdown-item @click="openExternal('http://localhost:9411')">
                  🔍 Zipkin 链路追踪 (:9411)
                </el-dropdown-item>
                <el-dropdown-item @click="openExternal('http://localhost:3000')">
                  📊 Grafana 监控大盘 (:3000)
                </el-dropdown-item>
                <el-dropdown-item @click="openExternal('http://localhost:9090')">
                  📈 Prometheus (:9090)
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 全屏切换 -->
          <el-tooltip content="切换全屏" placement="bottom">
            <el-button circle size="small" :icon="FullScreen" @click="toggleFullScreen" />
          </el-tooltip>

          <el-divider direction="vertical" />

          <!-- 数字化时钟 -->
          <div class="clock-wrap">
            <span class="pulse-dot pulse-dot-primary" style="margin-right: 6px"></span>
            <span class="clock-digits">{{ now }}</span>
          </div>
        </div>
      </el-header>

      <!-- 主视图区域 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- Nacos 配置管理弹窗 -->
    <NacosNoticeDialog v-model:visible="noticeDialogVisible" />
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, var(--aside-bg-start) 0%, var(--aside-bg-middle) 50%, var(--aside-bg-end) 100%);
  color: #fff;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-x: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px 16px;
  cursor: pointer;
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
  flex-shrink: 0;
}

.brand-text {
  flex: 1;
  min-width: 0;
}

.brand-title {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 11.5px;
  color: #94a3b8;
  margin-top: 1px;
}

.service-status-bar {
  margin: 0 14px 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-text {
  font-size: 11.5px;
  color: #cbd5e1;
  font-weight: 500;
}

.menu {
  border-right: none;
  padding: 0 10px;
  flex: 1;
}

.menu :deep(.el-menu-item) {
  height: 48px;
  margin: 4px 0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
  font-weight: 600;
}

.menu :deep(.el-icon) {
  font-size: 18px;
  margin-right: 8px;
}

.aside-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(0, 0, 0, 0.15);
}

.admin-profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0ea5e9, #38bdf8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
}

.admin-name {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
}

.admin-role {
  font-size: 11px;
  color: #94a3b8;
}

.collapse-btn {
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #fff;
}

/* ===== 顶栏 ===== */
.right-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.header {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  z-index: 5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.notice-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.notice-pill:hover {
  background: #e0e7ff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

.notice-badge {
  color: var(--el-color-primary);
  font-size: 15px;
  display: flex;
  align-items: center;
}

.notice-text {
  font-size: 12.5px;
  color: #334155;
  font-weight: 500;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-edit-btn {
  font-size: 11.5px;
  font-weight: 600;
  padding: 0;
}

.infra-btn {
  border-radius: 20px;
  font-size: 12.5px;
}

.clock-wrap {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
  font-family: monospace;
}

/* 主内容区 */
.main-content {
  padding: 22px;
  overflow-y: auto;
  background: var(--page-bg);
  flex: 1;
}

/* 路由转场动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
