<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Goods,
  ShoppingCart,
  Box,
  TrendCharts,
  Lightning,
  Plus,
  Refresh,
  Connection,
  Cpu,
  Monitor,
} from '@element-plus/icons-vue'
import { listProducts } from '@/api/product'
import { listOrders } from '@/api/order'
import { getStock } from '@/api/stock'
import type { Product, Order, Stock } from '@/types'

const router = useRouter()

const loading = ref(false)
const products = ref<Product[]>([])
const orders = ref<Order[]>([])
const stocks = ref<Record<string, Stock>>({})

// 销售额与核心统计
const totalGmv = computed(() =>
  orders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
)
const totalOrders = computed(() => orders.value.length)
const totalProducts = computed(() => products.value.length)

// 库存告警数 (<= 5 件)
const lowStockCount = computed(() => {
  let count = 0
  for (const p of products.value) {
    const s = stocks.value[p.id]
    if (s && s.quantity <= 5) count++
  }
  return count
})

// ECharts DOM 引用
const trendChartRef = ref<HTMLDivElement>()
const stockChartRef = ref<HTMLDivElement>()
let trendChartInstance: echarts.ECharts | null = null
let stockChartInstance: echarts.ECharts | null = null

async function loadData() {
  loading.value = true
  try {
    const [pList, oList] = await Promise.all([listProducts(), listOrders()])
    products.value = pList
    orders.value = oList

    // 异步拉取各个商品的库存
    const stockMap: Record<string, Stock> = {}
    await Promise.all(
      pList.map(async (p) => {
        try {
          const s = await getStock(p.id, true)
          if (s) stockMap[p.id] = s
        } catch {
          // ignore silent
        }
      })
    )
    stocks.value = stockMap

    await nextTick()
    initTrendChart()
    initStockChart()
  } finally {
    loading.value = false
  }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChartInstance) {
    trendChartInstance.dispose()
  }
  trendChartInstance = echarts.init(trendChartRef.value)

  // 模拟近 7 日趋势数据
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '今天']
  const salesData = [1240, 2300, 1890, 3400, 4200, 5600, totalGmv.value || 3890]
  const orderCountData = [5, 12, 8, 16, 22, 28, totalOrders.value || 19]

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.85)',
      borderColor: '#334155',
      textStyle: { color: '#f8fafc' },
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['成交总额 (￥)', '订单总数 (笔)'],
      right: '4%',
      top: '0%',
      textStyle: { color: '#64748b' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '14%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: days,
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#64748b' },
    },
    yAxis: [
      {
        type: 'value',
        name: '成交额 (元)',
        nameTextStyle: { color: '#94a3b8' },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' },
      },
      {
        type: 'value',
        name: '订单数 (笔)',
        nameTextStyle: { color: '#94a3b8' },
        splitLine: { show: false },
        axisLabel: { color: '#64748b' },
      },
    ],
    series: [
      {
        name: '成交总额 (￥)',
        type: 'line',
        smooth: true,
        data: salesData,
        itemStyle: { color: '#4f46e5' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(79, 70, 229, 0.45)' },
            { offset: 1, color: 'rgba(79, 70, 229, 0.02)' },
          ]),
        },
      },
      {
        name: '订单总数 (笔)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: orderCountData,
        itemStyle: { color: '#06b6d4' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(6, 182, 212, 0.35)' },
            { offset: 1, color: 'rgba(6, 182, 212, 0.01)' },
          ]),
        },
      },
    ],
  }

  trendChartInstance.setOption(option)
}

function initStockChart() {
  if (!stockChartRef.value) return
  if (stockChartInstance) {
    stockChartInstance.dispose()
  }
  stockChartInstance = echarts.init(stockChartRef.value)

  let adequateCount = 0
  let warningCount = 0
  let emptyCount = 0

  products.value.forEach((p) => {
    const s = stocks.value[p.id]
    if (!s || s.quantity === 0) {
      emptyCount++
    } else if (s.quantity <= 5) {
      warningCount++
    } else {
      adequateCount++
    }
  })

  // 默认占位避免空图
  if (products.value.length === 0) {
    adequateCount = 6
    warningCount = 2
    emptyCount = 1
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.85)',
      textStyle: { color: '#f8fafc' },
    },
    legend: {
      bottom: '5%',
      left: 'center',
      textStyle: { color: '#64748b' },
    },
    series: [
      {
        name: '库存分布',
        type: 'pie',
        radius: ['45%', '72%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: [
          { value: adequateCount, name: '库存充足 (>5)', itemStyle: { color: '#10b981' } },
          { value: warningCount, name: '库存预警 (1-5)', itemStyle: { color: '#f59e0b' } },
          { value: emptyCount, name: '已售罄/未配', itemStyle: { color: '#ef4444' } },
        ],
      },
    ],
  }

  stockChartInstance.setOption(option)
}

function handleResize() {
  trendChartInstance?.resize()
  stockChartInstance?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
  stockChartInstance?.dispose()
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 顶部核心指标看板 -->
    <div class="metrics-grid">
      <!-- 指标 1：累计 GMV -->
      <div class="metric-card gmv-card tech-card tech-card-hover">
        <div class="metric-header">
          <span class="metric-title">累计成交 GMV</span>
          <div class="metric-icon-wrap gmv-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
        </div>
        <div class="metric-body">
          <div class="metric-value">
            <span class="currency">￥</span>{{ totalGmv.toFixed(2) }}
          </div>
          <div class="metric-trend up">
            <span class="trend-badge">+16.8%</span>
            <span class="trend-text">较上周环比提升</span>
          </div>
        </div>
      </div>

      <!-- 指标 2：总订单数 -->
      <div class="metric-card tech-card tech-card-hover">
        <div class="metric-header">
          <span class="metric-title">全链路订单总数</span>
          <div class="metric-icon-wrap order-icon">
            <el-icon><ShoppingCart /></el-icon>
          </div>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ totalOrders }} <span class="unit">笔</span></div>
          <div class="metric-trend">
            <span class="pulse-dot pulse-dot-success" style="margin-right: 6px"></span>
            <span class="trend-text">Seata 2PC 保证数据一致性</span>
          </div>
        </div>
      </div>

      <!-- 指标 3：上架商品 -->
      <div class="metric-card tech-card tech-card-hover">
        <div class="metric-header">
          <span class="metric-title">在售商品总数 (SKU)</span>
          <div class="metric-icon-wrap product-icon">
            <el-icon><Goods /></el-icon>
          </div>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ totalProducts }} <span class="unit">款</span></div>
          <div class="metric-trend">
            <span class="trend-text">Feign 跨服务 RPC 实时调用</span>
          </div>
        </div>
      </div>

      <!-- 指标 4：库存预警与网关 -->
      <div class="metric-card tech-card tech-card-hover">
        <div class="metric-header">
          <span class="metric-title">库存告警指数</span>
          <div class="metric-icon-wrap warning-icon">
            <el-icon><Box /></el-icon>
          </div>
        </div>
        <div class="metric-body">
          <div class="metric-value" :class="{ 'text-danger': lowStockCount > 0 }">
            {{ lowStockCount }} <span class="unit">款需补货</span>
          </div>
          <div class="metric-trend">
            <span class="pulse-dot pulse-dot-warning" style="margin-right: 6px"></span>
            <span class="trend-text">支持行内一键秒级入库</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 中部图表区 -->
    <div class="charts-grid">
      <!-- 走势折线图 -->
      <div class="chart-box tech-card">
        <div class="chart-head">
          <div class="head-left">
            <div class="title-bar"></div>
            <span class="head-title">近 7 日交易额与下单趋势</span>
          </div>
          <el-button link type="primary" :icon="Refresh" @click="loadData">刷新数据</el-button>
        </div>
        <div ref="trendChartRef" class="chart-content"></div>
      </div>

      <!-- 库存分布图 -->
      <div class="chart-box tech-card">
        <div class="chart-head">
          <div class="head-left">
            <div class="title-bar emerald"></div>
            <span class="head-title">库存健康度分布</span>
          </div>
        </div>
        <div ref="stockChartRef" class="chart-content"></div>
      </div>
    </div>

    <!-- 微服务架构拓扑与链路概览 -->
    <div class="topology-box tech-card">
      <div class="topology-head">
        <div class="head-left">
          <div class="topology-icon">
            <el-icon><Connection /></el-icon>
          </div>
          <div>
            <div class="topo-title">Spring Cloud Alibaba 微服务全链路拓扑</div>
            <div class="topo-sub">统一网关 ➔ 订单服务 (Seata TM) ➔ 商品服务 (Feign) ➔ 库存服务 (Seata RM)</div>
          </div>
        </div>
        <el-button type="primary" plain size="small" :icon="Monitor" @click="router.push('/governance')">
          微服务治理与拓扑中心 ➔
        </el-button>
      </div>

      <div class="topo-nodes-wrapper">
        <div class="topo-node gateway-node">
          <div class="node-badge">统一入口</div>
          <div class="node-title">🚪 Gateway</div>
          <div class="node-port">:8080</div>
          <div class="node-tag">Sentinel 限流防护</div>
        </div>

        <div class="topo-arrow">➔</div>

        <div class="topo-node order-node">
          <div class="node-badge">全局事务 TM</div>
          <div class="node-title">📦 service-order</div>
          <div class="node-port">:8083</div>
          <div class="node-tag">@GlobalTransactional</div>
        </div>

        <div class="topo-arrow">➔</div>

        <div class="topo-node product-node">
          <div class="node-badge">远程调用 RPC</div>
          <div class="node-title">📦 service-product</div>
          <div class="node-port">:8081</div>
          <div class="node-tag">OpenFeign / Nacos</div>
        </div>

        <div class="topo-arrow">➔</div>

        <div class="topo-node stock-node">
          <div class="node-badge">资源管理器 RM</div>
          <div class="node-title">📦 service-stock</div>
          <div class="node-port">:8082</div>
          <div class="node-tag">undo_log 自动回滚</div>
        </div>
      </div>
    </div>

    <!-- 底部快捷操作卡片 -->
    <div class="quick-actions-grid">
      <div class="action-card tech-card tech-card-hover" @click="router.push('/order')">
        <div class="action-icon order-bg"><el-icon><Lightning /></el-icon></div>
        <div class="action-text">
          <div class="action-title">极速下单体验</div>
          <div class="action-desc">走 Seata 2PC 分布式事务全链路下单扣库</div>
        </div>
      </div>

      <div class="action-card tech-card tech-card-hover" @click="router.push('/stock')">
        <div class="action-icon stock-bg"><el-icon><Box /></el-icon></div>
        <div class="action-text">
          <div class="action-title">智能库存管控</div>
          <div class="action-desc">新商品开户与极速 +5/+10 一键补货</div>
        </div>
      </div>

      <div class="action-card tech-card tech-card-hover" @click="router.push('/products')">
        <div class="action-icon product-bg"><el-icon><Plus /></el-icon></div>
        <div class="action-text">
          <div class="action-title">商品资产中心</div>
          <div class="action-desc">卡片/表格双视图浏览与多维组合检索</div>
        </div>
      </div>

      <div class="action-card tech-card tech-card-hover" @click="router.push('/governance')">
        <div class="action-icon topo-bg"><el-icon><Cpu /></el-icon></div>
        <div class="action-text">
          <div class="action-title">Sentinel 限流演练</div>
          <div class="action-desc">高频并发压测体验网关 429 流量自愈</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 顶部指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

.metric-card {
  padding: 20px 22px;
}

.metric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-secondary);
}

.metric-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.gmv-icon {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.order-icon {
  background: linear-gradient(135deg, #0284c7, #06b6d4);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3);
}

.product-icon {
  background: linear-gradient(135deg, #059669, #10b981);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.warning-icon {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3);
}

.metric-body {
  margin-top: 14px;
}

.metric-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.5px;
}

.metric-value .currency {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-right: 2px;
}

.metric-value .unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

.text-danger {
  color: #ef4444 !important;
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
}

.trend-badge {
  background: #ecfdf5;
  color: #059669;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.trend-text {
  color: var(--text-secondary);
}

/* 图表区 */
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}
@media (max-width: 1024px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.chart-box {
  padding: 20px 22px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-bar {
  width: 4px;
  height: 16px;
  background: var(--el-color-primary);
  border-radius: 2px;
}

.title-bar.emerald {
  background: #10b981;
}

.head-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.chart-content {
  width: 100%;
  height: 280px;
}

/* 微服务架构拓扑大卡片 */
.topology-box {
  padding: 20px 24px;
}

.topology-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.topology-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.topo-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.topo-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.topo-nodes-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  overflow-x: auto;
  padding: 6px 0;
}

.topo-node {
  flex: 1;
  min-width: 150px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px 16px;
  position: relative;
  text-align: center;
  transition: all 0.2s;
}

.topo-node:hover {
  transform: translateY(-2px);
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.node-badge {
  font-size: 11px;
  font-weight: 600;
  color: #6366f1;
  background: #eef2ff;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.node-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.node-port {
  font-family: monospace;
  font-size: 12px;
  color: #64748b;
  margin: 2px 0 6px;
}

.node-tag {
  font-size: 11px;
  color: #059669;
  background: #ecfdf5;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.topo-arrow {
  font-size: 20px;
  color: #94a3b8;
  font-weight: 700;
}

/* 底部快捷操作 */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1024px) {
  .quick-actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .quick-actions-grid {
    grid-template-columns: 1fr;
  }
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  cursor: pointer;
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}

.order-bg {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.stock-bg {
  background: linear-gradient(135deg, #059669, #10b981);
}
.product-bg {
  background: linear-gradient(135deg, #0284c7, #38bdf8);
}
.topo-bg {
  background: linear-gradient(135deg, #d97706, #fbbf24);
}

.action-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.action-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 3px;
}
</style>
