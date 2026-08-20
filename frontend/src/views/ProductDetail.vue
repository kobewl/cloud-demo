<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, ShoppingCart, Refresh, Box } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/product'
import { createOrder } from '@/api/order'
import type { ProductDetail } from '@/types'

/**
 * 商品详情页：展示商品信息 + 当前库存，并提供"下单"操作。
 * 下单走 POST /api/order/create（Seata 分布式事务：查商品 -> 扣库存 -> 落订单），
 * 成功后提示订单 ID 并刷新库存，直观看到库存被扣减。
 */
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const detail = ref<ProductDetail | null>(null)

// 下单表单：购买数量
const orderForm = reactive({
  count: 1,
})

const productId = route.params.id as string

/** 加载商品详情（含库存） */
async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getProductDetail(productId)
  } finally {
    loading.value = false
  }
}

/** 提交下单（走 Seata 分布式事务） */
async function submitOrder() {
  submitting.value = true
  try {
    const orderId = await createOrder({ productId, count: orderForm.count })
    ElMessage.success(`下单成功！订单 ID：${orderId}`)
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page">
    <div class="top-bar">
      <el-button :icon="Back" @click="router.back()">返回列表</el-button>
    </div>

    <div v-loading="loading" class="detail-grid" v-if="detail">
      <!-- 左侧：商品信息 -->
      <div class="card product-card">
        <div class="product-hero">
          <div class="product-avatar">{{ detail.product.name?.charAt(0) ?? '?' }}</div>
          <div>
            <h2 class="product-name">{{ detail.product.name }}</h2>
            <div class="product-price">￥{{ Number(detail.product.price).toFixed(2) }}</div>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="1" class="meta">
          <el-descriptions-item label="商品 ID">{{ detail.product.id }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.product.createdAt ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="描述">
            <span class="desc">{{ detail.product.description || '暂无描述' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 右侧：库存 + 下单 -->
      <div class="right-col">
        <div class="card stock-card">
          <div class="stock-header">
            <span class="section-title"><el-icon><Box /></el-icon> 当前库存</span>
            <el-button link type="primary" :icon="Refresh" @click="loadDetail">刷新</el-button>
          </div>

          <div v-if="detail.stock" class="stock-body">
            <div class="stock-num" :class="{ low: detail.stock.quantity < 5 }">
              {{ detail.stock.quantity }}
            </div>
            <div class="stock-unit">件</div>
            <el-tag v-if="detail.stock.quantity < 5" type="danger" size="small">库存紧张</el-tag>
          </div>
          <el-empty v-else description="该商品还没有库存记录" :image-size="70" />
        </div>

        <div class="card order-card">
          <div class="section-title"><el-icon><ShoppingCart /></el-icon> 立即下单</div>
          <div class="order-row">
            <span class="order-label">购买数量</span>
            <el-input-number v-model="orderForm.count" :min="1" :max="999" />
          </div>
          <el-button
            type="primary"
            size="large"
            class="order-btn"
            :loading="submitting"
            :disabled="!detail.stock || detail.stock.quantity < 1"
            :icon="ShoppingCart"
            @click="submitOrder"
          >
            立即下单（Seata 分布式事务）
          </el-button>
          <div v-if="!detail.stock" class="hint">该商品还没有库存，请先到「库存配置」页配置</div>
          <div v-else-if="detail.stock.quantity < 1" class="hint">库存不足，无法下单</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.top-bar {
  display: flex;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.section-title .el-icon {
  color: var(--el-color-primary);
}

/* 商品 hero */
.product-hero {
  display: flex;
  align-items: center;
  gap: 20px;
}
.product-avatar {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  flex-shrink: 0;
}
.product-name {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}
.product-price {
  font-size: 26px;
  font-weight: 700;
  color: #ef4444;
}
.desc {
  color: #6b7280;
  line-height: 1.6;
}
.meta :deep(.el-descriptions__label) {
  color: #9ca3af;
  width: 90px;
}

/* 库存卡片 */
.stock-card {
  margin-bottom: 20px;
}
.stock-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stock-body {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 16px 0 4px;
}
.stock-num {
  font-size: 52px;
  font-weight: 700;
  color: #10b981;
  line-height: 1;
}
.stock-num.low {
  color: #ef4444;
}
.stock-unit {
  font-size: 15px;
  color: #9ca3af;
}

/* 下单卡片 */
.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0;
}
.order-label {
  color: #6b7280;
}
.order-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
}
.hint {
  margin-top: 12px;
  font-size: 13px;
  color: #e6a23c;
  text-align: center;
}
</style>
