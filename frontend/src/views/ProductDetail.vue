<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import confetti from 'canvas-confetti'
import {
  Back,
  ShoppingCart,
  Refresh,
  Box,
  Share,
  CircleCheckFilled,
  Coin,
  Discount,
  DocumentCopy,
} from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/product'
import { createOrder } from '@/api/order'
import { addStock, createStock } from '@/api/stock'
import SeataTraceDrawer from '@/components/SeataTraceDrawer.vue'
import type { ProductDetail, Order } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const detail = ref<ProductDetail | null>(null)
const lastCreatedOrder = ref<Order | null>(null)
const traceDrawerVisible = ref(false)

// 优惠券减免模拟
const discountActive = ref(true)

// 下单表单
const orderForm = reactive({
  count: 1,
})

const productId = route.params.id as string

// 计算小计与最终付款金额
const unitPrice = computed(() => Number(detail.value?.product.price || 0))
const rawTotal = computed(() => unitPrice.value * orderForm.count)
const discountAmount = computed(() => (discountActive.value && rawTotal.value >= 100 ? 10 : 0))
const finalPayAmount = computed(() => Math.max(0, rawTotal.value - discountAmount.value))

/** 加载商品详情 */
async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getProductDetail(productId)
  } finally {
    loading.value = false
  }
}

/** 触发粒子礼花动效 */
function fireConfetti() {
  confetti({
    particleCount: 80,
    spread: 70,
    origin: { y: 0.6 },
    colors: ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b'],
  })
}

/** 提交下单（Seata 2PC 分布式事务） */
async function submitOrder() {
  if (!detail.value) return
  submitting.value = true
  try {
    const orderId = await createOrder({ productId, count: orderForm.count })
    fireConfetti()
    ElMessage.success({
      message: `下单成功！Seata 全局事务已提交，订单号: ${orderId}`,
      duration: 5000,
    })

    // 构建用于抽屉展示的订单对象
    lastCreatedOrder.value = {
      id: orderId,
      productId: detail.value.product.id,
      productName: detail.value.product.name,
      price: detail.value.product.price,
      count: orderForm.count,
      totalAmount: finalPayAmount.value,
      status: 0,
      createdAt: new Date().toLocaleString(),
    }

    // 重新加载详情以查看扣减后的最新库存
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

/** 快捷补货 */
async function quickRestock(amount = 10) {
  try {
    if (!detail.value?.stock) {
      await createStock({ productId, quantity: amount })
      ElMessage.success(`初始库存建立成功 (+${amount} 件)`)
    } else {
      await addStock(productId, amount)
      ElMessage.success(`库存成功增加 +${amount} 件`)
    }
    await loadDetail()
  } catch {
    ElMessage.error('补货失败')
  }
}

function copyId(id: string) {
  navigator.clipboard.writeText(id)
  ElMessage.success('商品 ID 已复制')
}

onMounted(loadDetail)
</script>

<template>
  <div class="product-detail-page" v-loading="loading">
    <!-- 顶部返回导航 -->
    <div class="top-nav">
      <el-button :icon="Back" @click="router.push('/products')">返回商品列表</el-button>
      <div class="nav-breadcrumbs">
        <span>商品中心</span>
        <span class="sep">/</span>
        <span class="curr">{{ detail?.product.name || '商品全景工作台' }}</span>
      </div>
      <div class="spacer" />
      <el-button :icon="Refresh" @click="loadDetail">刷新详情</el-button>
    </div>

    <div v-if="detail" class="content-grid">
      <!-- 左侧：商品全景展示与微服务架构信息 -->
      <div class="left-col">
        <!-- 商品信息卡片 -->
        <div class="product-hero-card tech-card">
          <div class="hero-top">
            <div class="hero-avatar">{{ detail.product.name?.charAt(0) ?? '?' }}</div>
            <div class="hero-meta">
              <div class="product-tag-row">
                <el-tag effect="light" type="primary">微服务商品</el-tag>
                <el-tag effect="light" type="info">OpenFeign 接口</el-tag>
              </div>
              <h1 class="product-name">{{ detail.product.name }}</h1>
              <div class="price-container">
                <span class="currency">￥</span>
                <span class="price-val">{{ Number(detail.product.price).toFixed(2) }}</span>
                <span class="price-unit">/ 件</span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="desc-box">
            <div class="desc-label">商品详细描述</div>
            <div class="desc-content">
              {{ detail.product.description || '暂无详细描述，系统已接入分布式统一配置中心与商品微服务。' }}
            </div>
          </div>

          <el-descriptions :column="2" border size="small" class="meta-table">
            <el-descriptions-item label="商品全局 ID" :span="2">
              <span class="mono">{{ detail.product.id }}</span>
              <el-button link type="primary" size="small" :icon="DocumentCopy" @click="copyId(detail.product.id)">
                复制
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="服务提供者">
              <span class="badge-service">service-product:8081</span>
            </el-descriptions-item>
            <el-descriptions-item label="注册中心">
              <span class="badge-nacos">Nacos (cloud-demo)</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">
              {{ detail.product.createdAt || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Seata 2PC 流程提示卡片 -->
        <div class="seata-hint-card tech-card">
          <div class="hint-header">
            <div class="hint-icon"><el-icon><Share /></el-icon></div>
            <div>
              <div class="hint-title">Seata 2PC 分布式事务保障</div>
              <div class="hint-sub">下单操作将跨 service-order 与 service-stock 执行两阶段提交</div>
            </div>
          </div>
          <div class="step-flow">
            <div class="flow-item">
              <div class="flow-num">1</div>
              <div class="flow-text">TM 开启全局事务</div>
            </div>
            <div class="flow-arrow">➔</div>
            <div class="flow-item">
              <div class="flow-num">2</div>
              <div class="flow-text">RM 扣减库存</div>
            </div>
            <div class="flow-arrow">➔</div>
            <div class="flow-item">
              <div class="flow-num">3</div>
              <div class="flow-text">RM 创建订单</div>
            </div>
            <div class="flow-arrow">➔</div>
            <div class="flow-item">
              <div class="flow-num">4</div>
              <div class="flow-text">TC 全局提交</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：实时库存与下单工作台 -->
      <div class="right-col">
        <!-- 实时库存卡片 -->
        <div class="stock-panel tech-card">
          <div class="panel-head">
            <div class="panel-title">
              <el-icon><Box /></el-icon>
              <span>实时库存水位</span>
            </div>
            <el-button link type="primary" size="small" @click="quickRestock(10)">
              +10 快捷补货
            </el-button>
          </div>

          <div v-if="detail.stock" class="stock-meter-area">
            <div class="stock-digit-row">
              <div
                class="stock-number"
                :class="{
                  'is-adequate': detail.stock.quantity > 5,
                  'is-low': detail.stock.quantity > 0 && detail.stock.quantity <= 5,
                  'is-empty': detail.stock.quantity === 0,
                }"
              >
                {{ detail.stock.quantity }}
              </div>
              <div class="stock-unit">件可用</div>
              <el-tag
                v-if="detail.stock.quantity > 5"
                type="success"
                effect="light"
                round
                size="small"
              >
                库存充足
              </el-tag>
              <el-tag
                v-else-if="detail.stock.quantity > 0"
                type="warning"
                effect="light"
                round
                size="small"
              >
                库存紧张
              </el-tag>
              <el-tag v-else type="danger" effect="light" round size="small">
                已售罄
              </el-tag>
            </div>

            <el-progress
              :percentage="Math.min(100, detail.stock.quantity * 5)"
              :status="
                detail.stock.quantity === 0
                  ? 'exception'
                  : detail.stock.quantity <= 5
                  ? 'warning'
                  : 'success'
              "
              :stroke-width="8"
            />
          </div>

          <div v-else class="empty-stock">
            <el-empty description="该商品尚未在 service-stock 初始化库存" :image-size="60" />
            <el-button type="primary" plain size="small" :icon="Box" @click="quickRestock(20)">
              立即一键初始化库存 (20件)
            </el-button>
          </div>
        </div>

        <!-- 下单收银台卡片 -->
        <div class="checkout-panel tech-card">
          <div class="panel-head">
            <div class="panel-title">
              <el-icon><ShoppingCart /></el-icon>
              <span>Seata 极速下单收银台</span>
            </div>
          </div>

          <div class="checkout-body">
            <!-- 购买数量 -->
            <div class="form-row">
              <span class="row-label">购买数量</span>
              <el-input-number
                v-model="orderForm.count"
                :min="1"
                :max="detail.stock?.quantity || 1"
                :disabled="!detail.stock || detail.stock.quantity < 1"
                style="width: 140px"
              />
            </div>

            <!-- 优惠券模拟 -->
            <div class="form-row coupon-row">
              <span class="row-label">
                <el-icon><Discount /></el-icon> 满 100 减 10 优惠
              </span>
              <el-switch v-model="discountActive" />
            </div>

            <!-- 金额明细 -->
            <div class="bill-box">
              <div class="bill-item">
                <span>商品总额</span>
                <span>￥{{ rawTotal.toFixed(2) }}</span>
              </div>
              <div v-if="discountAmount > 0" class="bill-item discount">
                <span>优惠减免</span>
                <span>-￥{{ discountAmount.toFixed(2) }}</span>
              </div>
              <el-divider style="margin: 8px 0" />
              <div class="bill-item total">
                <span>应付总额</span>
                <span class="final-price">￥{{ finalPayAmount.toFixed(2) }}</span>
              </div>
            </div>

            <!-- 下单按钮 -->
            <el-button
              type="primary"
              size="large"
              class="submit-order-btn"
              :loading="submitting"
              :disabled="!detail.stock || detail.stock.quantity < 1"
              :icon="CircleCheckFilled"
              @click="submitOrder"
            >
              立即提交订单 (Seata 2PC)
            </el-button>

            <!-- 刚下的订单追踪按钮 -->
            <div v-if="lastCreatedOrder" class="recent-order-box">
              <div class="recent-title">
                <span class="pulse-dot pulse-dot-success" style="margin-right: 6px"></span>
                最近下单成功：订单 ID <code>{{ lastCreatedOrder.id.slice(-8) }}</code>
              </div>
              <el-button
                link
                type="primary"
                size="small"
                :icon="Share"
                @click="traceDrawerVisible = true"
              >
                查看 Seata 2PC 事务链路详情 ➔
              </el-button>
            </div>

            <div v-if="!detail.stock" class="status-tip warning">
              ⚠️ 该商品还没有库存记录，请先在上方点击“一键初始化库存”
            </div>
            <div v-else-if="detail.stock.quantity < 1" class="status-tip danger">
              🚫 当前库存为 0，无法下单，请先点击补货
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Seata 事务全链路追踪抽屉 -->
    <SeataTraceDrawer
      v-model:visible="traceDrawerVisible"
      :order="lastCreatedOrder"
    />
  </div>
</template>

<style scoped>
.product-detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-breadcrumbs {
  font-size: 14px;
  color: var(--text-secondary);
}

.nav-breadcrumbs .sep {
  margin: 0 8px;
  color: var(--text-muted);
}

.nav-breadcrumbs .curr {
  font-weight: 600;
  color: var(--text-primary);
}

.spacer {
  flex: 1;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.left-col,
.right-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 商品 Hero 卡片 */
.product-hero-card {
  padding: 24px;
}

.hero-top {
  display: flex;
  gap: 20px;
}

.hero-avatar {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 38px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3);
  flex-shrink: 0;
}

.hero-meta {
  flex: 1;
}

.product-tag-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.product-name {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
}

.price-container {
  display: flex;
  align-items: baseline;
  color: #ef4444;
}

.price-container .currency {
  font-size: 18px;
  font-weight: 700;
}

.price-container .price-val {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.5px;
}

.price-container .price-unit {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 4px;
}

.desc-box {
  background: #f8fafc;
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-bottom: 18px;
}

.desc-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.desc-content {
  font-size: 13.5px;
  color: var(--text-regular);
  line-height: 1.6;
}

.mono {
  font-family: monospace;
  font-size: 12.5px;
}

.badge-service {
  font-family: monospace;
  color: #4f46e5;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.badge-nacos {
  font-family: monospace;
  color: #0284c7;
  background: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

/* Seata 提示卡片 */
.seata-hint-card {
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  border: 1px solid #c7d2fe;
}

.hint-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.hint-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.hint-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #1e1b4b;
}

.hint-sub {
  font-size: 12px;
  color: #4338ca;
}

.step-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  background: #ffffff;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid #e0e7ff;
}

.flow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-align: center;
}

.flow-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4f46e5;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flow-text {
  font-size: 11px;
  color: var(--text-regular);
  font-weight: 500;
}

.flow-arrow {
  color: #94a3b8;
  font-weight: 700;
  font-size: 12px;
}

/* 右侧面板 */
.stock-panel,
.checkout-panel {
  padding: 20px 22px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-title .el-icon {
  color: var(--el-color-primary);
}

.stock-digit-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.stock-number {
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.stock-number.is-adequate {
  color: #10b981;
}
.stock-number.is-low {
  color: #f59e0b;
}
.stock-number.is-empty {
  color: #ef4444;
}

.stock-unit {
  font-size: 14px;
  color: var(--text-secondary);
}

.empty-stock {
  text-align: center;
  padding: 10px 0;
}

/* 收银台 */
.checkout-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.row-label {
  font-size: 13.5px;
  color: var(--text-regular);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.bill-box {
  background: #f8fafc;
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.bill-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 0;
}

.bill-item.discount {
  color: #10b981;
}

.bill-item.total {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.final-price {
  color: #ef4444;
  font-size: 18px;
  font-weight: 800;
}

.submit-order-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.recent-order-box {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-md);
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recent-title {
  font-size: 12.5px;
  color: #065f46;
  font-weight: 600;
}

.status-tip {
  font-size: 12px;
  text-align: center;
  padding: 8px;
  border-radius: var(--radius-sm);
}

.status-tip.warning {
  background: #fffbeb;
  color: #b45309;
}

.status-tip.danger {
  background: #fef2f2;
  color: #b91c1c;
}
</style>
