<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Connection,
  Cpu,
  Monitor,
  Lightning,
  Refresh,
  Check,
  Close,
  Link,
  Aim,
  Coin,
  Goods,
  Box,
  Document,
  Share,
} from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'
import { getNotice } from '@/api/config'
import axios from 'axios'

const shop = useShopStore()

// Sentinel 压测模拟状态
const testingSentinel = ref(false)
const sentinelResults = ref<
  { id: number; status: number; text: string; time: number; success: boolean }[]
>([])

// 基础设施控制台链接
const infraList = [
  {
    name: 'Nacos 控制台',
    port: '18080',
    url: 'http://localhost:18080',
    account: 'nacos / nacos',
    role: '服务注册与发现中心 / 动态配置管理',
    iconColor: '#0ea5e9',
  },
  {
    name: 'Zipkin 链路追踪',
    port: '9411',
    url: 'http://localhost:9411',
    account: '无需登录',
    role: '微服务全链路调用追踪 (Trace / Span)',
    iconColor: '#f97316',
  },
  {
    name: 'Grafana 指标大盘',
    port: '3000',
    url: 'http://localhost:3000',
    account: 'admin / admin',
    role: 'CPU/内存/QPS 性能大盘看板',
    iconColor: '#f59e0b',
  },
  {
    name: 'Prometheus 监控',
    port: '9090',
    url: 'http://localhost:9090',
    account: '无需登录',
    role: '时序指标采集引擎 (Actuator Pull)',
    iconColor: '#ef4444',
  },
]

/** 发起 Sentinel 网关并发压测 */
async function runSentinelTest() {
  testingSentinel.value = true
  sentinelResults.value = []

  const requests = Array.from({ length: 10 }, (_, i) => i + 1)
  const startTime = Date.now()

  // 1 秒内密集并发 10 个请求
  const promises = requests.map(async (num) => {
    const reqStart = Date.now()
    try {
      // 请求商品列表接口（网关路由配置了 QPS 限流）
      await axios.get('/api/product/list?test=sentinel_' + num, { timeout: 3000 })
      return {
        id: num,
        status: 200,
        text: '200 OK (正常放行)',
        time: Date.now() - reqStart,
        success: true,
      }
    } catch (err: any) {
      const status = err.response?.status || 500
      if (status === 429) {
        return {
          id: num,
          status: 429,
          text: '429 Too Many Requests (Sentinel 限流拦截)',
          time: Date.now() - reqStart,
          success: false,
        }
      }
      return {
        id: num,
        status: status,
        text: `${status} 网络/服务响应`,
        time: Date.now() - reqStart,
        success: false,
      }
    }
  })

  const results = await Promise.all(promises)
  sentinelResults.value = results
  testingSentinel.value = false

  const blockedCount = results.filter((r) => r.status === 429).length
  if (blockedCount > 0) {
    ElMessage.warning(`压测完成：共 10 次并发，其中 ${blockedCount} 次被 Sentinel 网关成功限流降级 (429)！`)
  } else {
    ElMessage.success('压测完成：全量请求已通过')
  }
}

function openExternal(url: string) {
  window.open(url, '_blank')
}

onMounted(() => {
  shop.fetchNotice()
})
</script>

<template>
  <div class="governance-page">
    <!-- 顶部 Banner -->
    <div class="top-banner tech-card">
      <div class="banner-left">
        <div class="banner-icon"><el-icon><Monitor /></el-icon></div>
        <div>
          <h2 class="banner-title">Spring Cloud Alibaba 微服务治理与拓扑中心</h2>
          <p class="banner-sub">
            集中纳管服务注册发现 (Nacos)、统一网关 (Gateway)、流量治理 (Sentinel)、分布式事务 (Seata)、链路追踪 (Zipkin)。
          </p>
        </div>
      </div>
      <div class="status-pill-group">
        <el-tag type="success" effect="light" round>
          <span class="pulse-dot pulse-dot-success" style="margin-right: 6px"></span>
          Nacos 服务已就绪
        </el-tag>
        <el-tag type="primary" effect="light" round>
          <span class="pulse-dot pulse-dot-primary" style="margin-right: 6px"></span>
          网关 Gateway 8080
        </el-tag>
      </div>
    </div>

    <!-- 1. 全景微服务架构拓扑交互图 -->
    <div class="section-card tech-card">
      <div class="card-head">
        <div class="head-left">
          <div class="head-icon"><el-icon><Connection /></el-icon></div>
          <div>
            <div class="head-title">微服务全链路架构拓扑</div>
            <div class="head-sub">基于 Nacos 注册中心的分组命名空间与 RPC 调用拓扑</div>
          </div>
        </div>
      </div>

      <div class="topology-canvas">
        <!-- 网关层 -->
        <div class="topo-layer">
          <div class="layer-title">客户端请求与网关路由层</div>
          <div class="layer-nodes">
            <div class="topo-item gateway-box">
              <div class="node-badge">统一接入</div>
              <div class="item-name">🚪 API Gateway</div>
              <div class="item-meta">Port: 8080 · 路由与鉴权</div>
              <div class="item-tag">Sentinel 限流拦截器</div>
            </div>
          </div>
        </div>

        <div class="layer-arrow">⬇ OpenFeign 跨服务 RPC 调用 / HTTP Forward</div>

        <!-- 业务服务层 -->
        <div class="topo-layer">
          <div class="layer-title">业务微服务层 (Spring Cloud Alibaba)</div>
          <div class="layer-nodes three-cols">
            <!-- Order -->
            <div class="topo-item order-box">
              <div class="node-badge tm">Seata TM</div>
              <div class="item-name">📦 service-order</div>
              <div class="item-meta">Port: 8083 · 订单中心</div>
              <div class="item-tag">@GlobalTransactional</div>
            </div>

            <!-- Product -->
            <div class="topo-item product-box">
              <div class="node-badge">Feign Client</div>
              <div class="item-name">📦 service-product</div>
              <div class="item-meta">Port: 8081 · 商品中心</div>
              <div class="item-tag">@RefreshScope 动态配置</div>
            </div>

            <!-- Stock -->
            <div class="topo-item stock-box">
              <div class="node-badge rm">Seata RM</div>
              <div class="item-name">📦 service-stock</div>
              <div class="item-meta">Port: 8082 · 库存中心</div>
              <div class="item-tag">undo_log 自动补偿</div>
            </div>
          </div>
        </div>

        <div class="layer-arrow">⬇ 注册发现 / 配置拉取 / 事务协调 / 数据持久化</div>

        <!-- 基础设施与中间件层 -->
        <div class="topo-layer">
          <div class="layer-title">基础设施与数据存储中间件</div>
          <div class="layer-nodes four-cols">
            <div class="topo-item infra-box nacos">
              <div class="item-name">🌐 Nacos</div>
              <div class="item-meta">:8848 (18080)</div>
              <div class="item-tag">注册与配置中心</div>
            </div>
            <div class="topo-item infra-box seata">
              <div class="item-name">⚖️ Seata TC</div>
              <div class="item-meta">:8091</div>
              <div class="item-tag">全局事务协调器</div>
            </div>
            <div class="topo-item infra-box mysql">
              <div class="item-name">🗄️ MySQL 8.0</div>
              <div class="item-meta">:3306</div>
              <div class="item-tag">product_db / stock_db</div>
            </div>
            <div class="topo-item infra-box zipkin">
              <div class="item-name">🔍 Zipkin</div>
              <div class="item-meta">:9411</div>
              <div class="item-tag">分布式链路追踪</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. Sentinel 限流演练与 Nacos 动态配置测试 -->
    <div class="two-col-grid">
      <!-- Sentinel 压测实验 -->
      <div class="section-card tech-card">
        <div class="card-head">
          <div class="head-left">
            <div class="head-icon warn-icon"><el-icon><Lightning /></el-icon></div>
            <div>
              <div class="head-title">Sentinel 网关流控与熔断演练</div>
              <div class="head-sub">网关配置了商品 5 QPS / 订单 2 QPS 限流规则</div>
            </div>
          </div>
          <el-button
            type="warning"
            :loading="testingSentinel"
            :icon="Lightning"
            @click="runSentinelTest"
          >
            发起并发压测 (10连发)
          </el-button>
        </div>

        <div class="sentinel-body">
          <p class="sentinel-intro">
            点击上方按钮，系统将在 1 秒内瞬间向网关发送 10 次并发请求。观察 Sentinel 是如何拦截超出 QPS 阈值的多余流量并返回 <code>429 Too Many Requests</code>，保障后端集群不被击垮。
          </p>

          <div v-if="sentinelResults.length > 0" class="results-grid">
            <div
              v-for="r in sentinelResults"
              :key="r.id"
              class="result-item"
              :class="{ blocked: r.status === 429, success: r.status === 200 }"
            >
              <div class="res-head">
                <span class="req-id">Req #{{ r.id }}</span>
                <el-tag :type="r.status === 200 ? 'success' : 'danger'" size="small">
                  HTTP {{ r.status }}
                </el-tag>
              </div>
              <div class="res-msg">{{ r.text }}</div>
              <div class="res-time">耗时: {{ r.time }}ms</div>
            </div>
          </div>
          <div v-else class="sentinel-placeholder">
            <span>点击「发起并发压测」立即观察网关流控防御效果</span>
          </div>
        </div>
      </div>

      <!-- 基础设施快速直达 -->
      <div class="section-card tech-card">
        <div class="card-head">
          <div class="head-left">
            <div class="head-icon link-icon"><el-icon><Link /></el-icon></div>
            <div>
              <div class="head-title">Docker 基础设施直达控制台</div>
              <div class="head-sub">微服务配套开发与观测可视化后台一键直达</div>
            </div>
          </div>
        </div>

        <div class="infra-grid">
          <div
            v-for="inf in infraList"
            :key="inf.name"
            class="infra-card"
            @click="openExternal(inf.url)"
          >
            <div class="infra-head">
              <div class="infra-name">{{ inf.name }}</div>
              <el-tag size="small" type="info">{{ inf.port }}</el-tag>
            </div>
            <div class="infra-role">{{ inf.role }}</div>
            <div class="infra-account">账号: <code>{{ inf.account }}</code></div>
            <div class="infra-foot">
              <el-link :href="inf.url" target="_blank" type="primary" :underline="false">
                打开控制台 ➔
              </el-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.governance-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.top-banner {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
  color: #fff;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.banner-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.banner-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.banner-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.status-pill-group {
  display: flex;
  gap: 8px;
}

/* 拓扑卡片 */
.section-card {
  padding: 20px 24px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.head-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.warn-icon {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.link-icon {
  background: linear-gradient(135deg, #0284c7, #38bdf8);
}

.head-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.head-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 拓扑图画板 */
.topology-canvas {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.topo-layer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.layer-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.layer-nodes {
  display: flex;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

.layer-nodes.three-cols {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  max-width: 900px;
}

.layer-nodes.four-cols {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  max-width: 900px;
}

.layer-arrow {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.topo-item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;
  transition: all 0.2s;
}

.topo-item:hover {
  transform: translateY(-2px);
  border-color: #6366f1;
  box-shadow: 0 8px 16px rgba(99, 102, 241, 0.15);
}

.gateway-box {
  width: 260px;
}

.node-badge {
  font-size: 10.5px;
  font-weight: 700;
  background: #eef2ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 6px;
}

.node-badge.tm {
  background: #ecfdf5;
  color: #059669;
}

.node-badge.rm {
  background: #fffbeb;
  color: #d97706;
}

.item-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.item-meta {
  font-family: monospace;
  font-size: 11.5px;
  color: var(--text-secondary);
  margin: 2px 0 6px;
}

.item-tag {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

/* 两列栅格 */
.two-col-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}
@media (max-width: 1024px) {
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}

.sentinel-intro {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 14px;
}

.sentinel-intro code {
  background: #fee2e2;
  color: #991b1b;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.result-item {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  background: #f8fafc;
}

.result-item.blocked {
  background: #fef2f2;
  border-color: #fca5a5;
}

.result-item.success {
  background: #ecfdf5;
  border-color: #86efac;
}

.res-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
}

.res-msg {
  font-size: 11.5px;
  margin: 4px 0;
  color: var(--text-regular);
}

.res-time {
  font-size: 11px;
  color: var(--text-muted);
}

.sentinel-placeholder {
  text-align: center;
  padding: 30px;
  color: var(--text-muted);
  font-size: 13px;
  background: #f8fafc;
  border-radius: var(--radius-md);
}

/* 基础设施卡片 */
.infra-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.infra-card {
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
}

.infra-card:hover {
  background: #fff;
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}

.infra-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.infra-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.infra-role {
  font-size: 11.5px;
  color: var(--text-secondary);
  margin: 6px 0;
}

.infra-account {
  font-size: 11.5px;
  color: var(--text-muted);
}

.infra-foot {
  margin-top: 8px;
  text-align: right;
}
</style>
