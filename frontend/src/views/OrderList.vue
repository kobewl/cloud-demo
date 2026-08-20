<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { List, Refresh } from '@element-plus/icons-vue'
import { listOrders } from '@/api/order'
import type { Order } from '@/types'

/**
 * 订单列表页：展示所有订单。
 * 每笔订单都是下单时通过 Seata 分布式事务生成的，可配合「下单成功→订单出现」观察全链路。
 */

// 订单状态的中文映射（后端：0-已创建 1-已支付 2-已取消）
const STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'danger' }> = {
  0: { text: '已创建', type: 'info' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已取消', type: 'danger' },
}

const loading = ref(false)
const orders = ref<Order[]>([])

// 统计：订单总数 + 总金额
const totalCount = computed(() => orders.value.length)
const totalAmount = computed(() =>
  orders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0),
)

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await listOrders()
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page">
    <!-- 统计卡片 -->
    <div class="stat-bar">
      <div class="stat-card">
        <div class="stat-icon orders"><el-icon><List /></el-icon></div>
        <div>
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">订单总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon amount"><el-icon><Money /></el-icon></div>
        <div>
          <div class="stat-value">￥{{ totalAmount.toFixed(2) }}</div>
          <div class="stat-label">成交总额</div>
        </div>
      </div>
    </div>

    <!-- 订单表格 -->
    <div class="card">
      <div class="toolbar">
        <span class="card-title">订单明细</span>
        <div class="spacer" />
        <el-button :icon="Refresh" @click="loadOrders">刷新</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="订单 ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column label="单价" width="110">
          <template #default="{ row }">￥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="count" label="数量" width="80" />
        <el-table-column label="总金额" width="130">
          <template #default="{ row }">
            <span class="amount">￥{{ Number(row.totalAmount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status]?.type ?? 'info'" effect="light" round>
              {{ STATUS_MAP[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <template #empty>
          <el-empty description="还没有订单，去商品详情页下一单试试" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.stat-bar {
  display: flex;
  gap: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  min-width: 220px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
}
.stat-icon.orders {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.stat-icon.amount {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}
.stat-label {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 6px;
}
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
}
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.spacer {
  flex: 1;
}
.amount {
  color: #ef4444;
  font-weight: 600;
}
</style>
