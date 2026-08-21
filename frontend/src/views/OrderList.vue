<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  List,
  Refresh,
  Money,
  Search,
  Share,
  DocumentCopy,
  Download,
  ShoppingCart,
  TrendCharts,
  CircleCheckFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import { listOrders } from '@/api/order'
import SeataTraceDrawer from '@/components/SeataTraceDrawer.vue'
import type { Order } from '@/types'

const router = useRouter()

const loading = ref(false)
const orders = ref<Order[]>([])
const activeTab = ref('all') // all | 0 | 1 | 2
const keyword = ref('')
const selectedOrder = ref<Order | null>(null)
const traceDrawerVisible = ref(false)

// 订单状态配置
const STATUS_CONFIG: Record<
  number,
  { label: string; tagType: 'info' | 'success' | 'danger'; desc: string }
> = {
  0: { label: '已创建 (待付款)', tagType: 'info', desc: 'Seata 分布式事务已落单并锁定库存' },
  1: { label: '已支付 (已完成)', tagType: 'success', desc: '交易成功完成' },
  2: { label: '已取消 (已回滚)', tagType: 'danger', desc: '订单关闭，库存已补偿释放' },
}

// 统计数据
const totalCount = computed(() => orders.value.length)
const totalAmount = computed(() =>
  orders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
)
const avgAmount = computed(() =>
  totalCount.value > 0 ? (totalAmount.value / totalCount.value).toFixed(2) : '0.00'
)

// 过滤后的订单
const filteredOrders = computed(() => {
  let list = orders.value

  // 状态筛选
  if (activeTab.value !== 'all') {
    const targetStatus = Number(activeTab.value)
    list = list.filter((o) => o.status === targetStatus)
  }

  // 关键词搜索
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (o) =>
        o.id.toLowerCase().includes(kw) ||
        (o.productName && o.productName.toLowerCase().includes(kw))
    )
  }

  return list
})

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await listOrders()
  } finally {
    loading.value = false
  }
}

function openTraceDrawer(order: Order) {
  selectedOrder.value = order
  traceDrawerVisible.value = true
}

function copyId(id: string) {
  navigator.clipboard.writeText(id)
  ElMessage.success('订单 ID 已复制到剪贴板')
}

function exportCSV() {
  if (orders.value.length === 0) {
    ElMessage.warning('暂无订单可导出')
    return
  }
  const headers = ['订单ID,商品名称,单价,数量,总金额,状态,创建时间']
  const rows = filteredOrders.value.map((o) => {
    const statusLabel = STATUS_CONFIG[o.status]?.label ?? o.status
    return `"${o.id}","${o.productName}",${o.price},${o.count},${o.totalAmount},"${statusLabel}","${o.createdAt || ''}"`
  })
  const csvContent = '\uFEFF' + headers.concat(rows).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `订单明细报表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('订单报表已成功导出 CSV')
}

onMounted(loadOrders)
</script>

<template>
  <div class="order-page" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-banner-grid">
      <div class="stat-item tech-card">
        <div class="stat-icon-wrap orders"><el-icon><List /></el-icon></div>
        <div>
          <div class="stat-num">{{ totalCount }} <span class="unit">笔</span></div>
          <div class="stat-lbl">全链路订单总数</div>
        </div>
      </div>

      <div class="stat-item tech-card">
        <div class="stat-icon-wrap amount"><el-icon><Money /></el-icon></div>
        <div>
          <div class="stat-num">￥{{ totalAmount.toFixed(2) }}</div>
          <div class="stat-lbl">累计交易成交 GMV</div>
        </div>
      </div>

      <div class="stat-item tech-card">
        <div class="stat-icon-wrap avg"><el-icon><TrendCharts /></el-icon></div>
        <div>
          <div class="stat-num">￥{{ avgAmount }}</div>
          <div class="stat-lbl">笔均客单价 (AOV)</div>
        </div>
      </div>
    </div>

    <!-- 订单主体卡片 -->
    <div class="main-card tech-card">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-radio-group v-model="activeTab" class="status-tabs">
            <el-radio-button label="all">全部 ({{ totalCount }})</el-radio-button>
            <el-radio-button label="0">已创建</el-radio-button>
            <el-radio-button label="1">已支付</el-radio-button>
            <el-radio-button label="2">已取消</el-radio-button>
          </el-radio-group>

          <el-input
            v-model="keyword"
            placeholder="搜索订单 ID / 商品名称"
            clearable
            class="search-box"
            :prefix-icon="Search"
          />
        </div>

        <div class="toolbar-right">
          <el-button :icon="Download" @click="exportCSV">导出报表</el-button>
          <el-button :icon="Refresh" @click="loadOrders">刷新</el-button>
          <el-button type="primary" :icon="ShoppingCart" @click="router.push('/order')">
            去下单
          </el-button>
        </div>
      </div>

      <el-table :data="filteredOrders" stripe style="width: 100%">
        <el-table-column prop="id" label="订单 ID" min-width="190">
          <template #default="{ row }">
            <div class="order-id-cell">
              <span class="mono">{{ row.id }}</span>
              <el-button link type="primary" size="small" :icon="DocumentCopy" @click="copyId(row.id)">
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="productName" label="购买商品" min-width="160">
          <template #default="{ row }">
            <span class="product-title">{{ row.productName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="商品单价" width="120">
          <template #default="{ row }">
            <span>￥{{ Number(row.price).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="count" label="数量" width="90">
          <template #default="{ row }">
            <strong>x {{ row.count }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="实付总金额" width="150" sortable prop="totalAmount">
          <template #default="{ row }">
            <strong class="price-cell">￥{{ Number(row.totalAmount).toFixed(2) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="订单状态" width="160">
          <template #default="{ row }">
            <el-tag
              :type="STATUS_CONFIG[row.status]?.tagType ?? 'info'"
              effect="light"
              round
            >
              {{ STATUS_CONFIG[row.status]?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="下单时间" min-width="170" />

        <el-table-column label="分布式事务与操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :icon="Share"
              @click="openTraceDrawer(row)"
            >
              Seata 事务链路
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无符合条件的订单记录，快去下一单试试吧！" />
        </template>
      </el-table>
    </div>

    <!-- Seata 事务全景追踪抽屉 -->
    <SeataTraceDrawer
      v-model:visible="traceDrawerVisible"
      :order="selectedOrder"
    />
  </div>
</template>

<style scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-banner-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 768px) {
  .stat-banner-grid {
    grid-template-columns: 1fr;
  }
}

.stat-item {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrap {
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

.stat-icon-wrap.orders {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.stat-icon-wrap.amount {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
}
.stat-icon-wrap.avg {
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
}

.stat-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}

.stat-num .unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
}

.stat-lbl {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.main-card {
  padding: 20px 24px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.search-box {
  width: 250px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mono {
  font-family: monospace;
  font-size: 12px;
  color: var(--text-regular);
}

.product-title {
  font-weight: 600;
  color: var(--text-primary);
}

.price-cell {
  color: #ef4444;
  font-size: 14px;
}
</style>
