<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import confetti from 'canvas-confetti'
import {
  ShoppingCart,
  Box,
  Share,
  CircleCheckFilled,
  Discount,
  Goods,
  List,
  DocumentCopy,
} from '@element-plus/icons-vue'
import { listProducts, getProductDetail } from '@/api/product'
import { createOrder } from '@/api/order'
import SeataTraceDrawer from '@/components/SeataTraceDrawer.vue'
import type { Product, ProductDetail, Order } from '@/types'

const router = useRouter()

const products = ref<Product[]>([])
const detail = ref<ProductDetail | null>(null)
const loadingDetail = ref(false)
const submitting = ref(false)
const lastOrder = ref<Order | null>(null)
const drawerVisible = ref(false)

const form = reactive({
  productId: '' as string,
  count: 1,
  discount: true,
})

/** 加载商品列表 */
async function loadProducts() {
  products.value = await listProducts()
  // 默认选中第一个有库存的商品
  if (products.value.length > 0 && !form.productId) {
    form.productId = products.value[0].id
  }
}

/** 加载选中商品的详情与库存 */
async function loadDetail() {
  if (!form.productId) {
    detail.value = null
    return
  }
  loadingDetail.value = true
  try {
    detail.value = await getProductDetail(form.productId)
  } finally {
    loadingDetail.value = false
  }
}

// 计算金额
const price = computed(() => Number(detail.value?.product.price || 0))
const totalBeforeDiscount = computed(() => price.value * form.count)
const discountCut = computed(() => (form.discount && totalBeforeDiscount.value >= 100 ? 10 : 0))
const payAmount = computed(() => Math.max(0, totalBeforeDiscount.value - discountCut.value))

function fireConfetti() {
  confetti({
    particleCount: 100,
    spread: 80,
    origin: { y: 0.6 },
    colors: ['#6366f1', '#06b6d4', '#10b981', '#f59e0b', '#ec4899'],
  })
}

/** 提交下单 */
async function submit() {
  if (!form.productId || !detail.value) {
    ElMessage.warning('请先选择商品')
    return
  }
  submitting.value = true
  try {
    const orderId = await createOrder({ productId: form.productId, count: form.count })
    fireConfetti()
    ElMessage.success(`下单成功！Seata 全局事务已完成，订单 ID: ${orderId}`)

    lastOrder.value = {
      id: orderId,
      productId: detail.value.product.id,
      productName: detail.value.product.name,
      price: detail.value.product.price,
      count: form.count,
      totalAmount: payAmount.value,
      status: 0,
      createdAt: new Date().toLocaleString(),
    }

    // 刷新库存
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

watch(() => form.productId, loadDetail)

onMounted(loadProducts)
</script>

<template>
  <div class="place-order-page">
    <!-- 顶部 Banner -->
    <div class="banner-card tech-card">
      <div class="banner-left">
        <div class="banner-icon">
          <el-icon><ShoppingCart /></el-icon>
        </div>
        <div>
          <h2 class="banner-title">Seata 分布式事务下单工作台</h2>
          <p class="banner-sub">
            跨服务协同：Feign 远程查商品 ➔ service-stock 扣减库存 ➔ service-order 落单，异常时由 Seata 自动回滚。
          </p>
        </div>
      </div>
      <el-button type="primary" plain :icon="List" @click="router.push('/orders')">
        查看全部订单
      </el-button>
    </div>

    <!-- 主体分栏 -->
    <div class="checkout-grid">
      <!-- 左侧：商品选择与收银结算 -->
      <div class="left-panel tech-card">
        <div class="panel-section-title">
          <el-icon><Goods /></el-icon>
          <span>1. 选择购买商品</span>
        </div>

        <!-- 快速商品卡片选择器 -->
        <div class="product-selector-grid">
          <div
            v-for="p in products"
            :key="p.id"
            class="picker-card"
            :class="{ active: form.productId === p.id }"
            @click="form.productId = p.id"
          >
            <div class="picker-avatar">{{ p.name.charAt(0) }}</div>
            <div class="picker-info">
              <div class="picker-name" :title="p.name">{{ p.name }}</div>
              <div class="picker-price">￥{{ Number(p.price).toFixed(2) }}</div>
            </div>
            <div v-if="form.productId === p.id" class="picker-check">✓</div>
          </div>
        </div>

        <el-divider />

        <!-- 选中商品状态与数量配置 -->
        <div v-if="detail" v-loading="loadingDetail" class="detail-section">
          <div class="panel-section-title">
            <el-icon><Box /></el-icon>
            <span>2. 配置订单明细</span>
          </div>

          <div class="selected-product-card">
            <div class="prod-badge-row">
              <span class="prod-title">{{ detail.product.name }}</span>
              <el-tag
                v-if="detail.stock && detail.stock.quantity > 5"
                type="success"
                effect="light"
                round
              >
                库存充足 ({{ detail.stock.quantity }} 件)
              </el-tag>
              <el-tag
                v-else-if="detail.stock && detail.stock.quantity > 0"
                type="warning"
                effect="light"
                round
              >
                仅剩 {{ detail.stock.quantity }} 件
              </el-tag>
              <el-tag v-else type="danger" effect="light" round>缺货中</el-tag>
            </div>
            <div class="prod-desc">{{ detail.product.description || '暂无商品补充描述' }}</div>
          </div>

          <div class="form-item-row">
            <span class="label">购买件数</span>
            <el-input-number
              v-model="form.count"
              :min="1"
              :max="detail.stock?.quantity || 1"
              :disabled="!detail.stock || detail.stock.quantity < 1"
            />
          </div>

          <div class="form-item-row">
            <span class="label">
              <el-icon><Discount /></el-icon> 满减特惠 (满100减10)
            </span>
            <el-switch v-model="form.discount" />
          </div>

          <!-- 账单明细 -->
          <div class="bill-summary">
            <div class="bill-row">
              <span>商品单价</span>
              <span>￥{{ price.toFixed(2) }}</span>
            </div>
            <div class="bill-row">
              <span>购买数量</span>
              <span>x {{ form.count }}</span>
            </div>
            <div v-if="discountCut > 0" class="bill-row cut">
              <span>优惠立减</span>
              <span>-￥{{ discountCut.toFixed(2) }}</span>
            </div>
            <el-divider style="margin: 8px 0" />
            <div class="bill-row grand-total">
              <span>应付总金额</span>
              <span class="price-text">￥{{ payAmount.toFixed(2) }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="checkout-submit-btn"
            :loading="submitting"
            :disabled="!detail.stock || detail.stock.quantity < 1"
            :icon="CircleCheckFilled"
            @click="submit"
          >
            立即提交订单 (Seata 2PC)
          </el-button>

          <!-- 下单成功提示条 -->
          <div v-if="lastOrder" class="last-order-banner">
            <div>
              <span class="pulse-dot pulse-dot-success" style="margin-right: 6px"></span>
              <strong>下单成功！</strong> 订单号: <code>{{ lastOrder.id }}</code>
            </div>
            <el-button link type="primary" :icon="Share" @click="drawerVisible = true">
              查看 Seata 事务全景追踪 ➔
            </el-button>
          </div>
        </div>

        <el-empty v-else description="请在上方选择一个商品进行结算" />
      </div>

      <!-- 右侧：Seata 2PC 流程图解与知识中心 -->
      <div class="right-panel tech-card">
        <div class="panel-section-title">
          <el-icon><Share /></el-icon>
          <span>Seata 2PC 分布式事务全景机制</span>
        </div>

        <div class="seata-diagram">
          <!-- Step 1 -->
          <div class="diagram-step">
            <div class="step-badge">Phase 1</div>
            <div class="step-box">
              <div class="step-name">1. TM 开启全局事务</div>
              <div class="step-role">service-order (@GlobalTransactional)</div>
              <div class="step-desc">向 Seata TC 申请生成全局唯一 XID，并在 RPC 协议头传递</div>
            </div>
          </div>

          <div class="diagram-connector">↓</div>

          <!-- Step 2 -->
          <div class="diagram-step">
            <div class="step-badge">Phase 1</div>
            <div class="step-box highlight-box">
              <div class="step-name">2. 分支 1: service-stock 扣减库存</div>
              <div class="step-role">Resource Manager (RM)</div>
              <div class="step-desc">记录 before-image & after-image 到 undo_log，释放本地锁</div>
            </div>
          </div>

          <div class="diagram-connector">↓</div>

          <!-- Step 3 -->
          <div class="diagram-step">
            <div class="step-badge">Phase 1</div>
            <div class="step-box highlight-box">
              <div class="step-name">3. 分支 2: service-order 落单</div>
              <div class="step-role">Resource Manager (RM)</div>
              <div class="step-desc">向 order_db 插入订单数据，写入本地 undo_log</div>
            </div>
          </div>

          <div class="diagram-connector">↓</div>

          <!-- Step 4 -->
          <div class="diagram-step">
            <div class="step-badge phase2">Phase 2</div>
            <div class="step-box commit-box">
              <div class="step-name">4. TC 异步两阶段提交 (Global Commit)</div>
              <div class="step-role">Seata Server 协调器</div>
              <div class="step-desc">若全链路无异常，TC 异步通知各 RM 删除 undo_log 极速完成提交！</div>
            </div>
          </div>
        </div>

        <div class="faq-box">
          <div class="faq-title">💡 为什么采用 Seata AT 模式？</div>
          <div class="faq-desc">
            业务无侵入，无需手写 try/confirm/cancel 补偿逻辑。基于底层数据库本地事务与 undo_log，性能极高且保证强一致性。
          </div>
        </div>
      </div>
    </div>

    <!-- 事务链路详情抽屉 -->
    <SeataTraceDrawer
      v-model:visible="drawerVisible"
      :order="lastOrder"
    />
  </div>
</template>

<style scoped>
.place-order-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.banner-card {
  padding: 18px 24px;
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
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
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

/* 分栏布局 */
.checkout-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}
@media (max-width: 1024px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}

.left-panel,
.right-panel {
  padding: 22px;
}

.panel-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.panel-section-title .el-icon {
  color: var(--el-color-primary);
}

/* 商品卡片选择器 */
.product-selector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
  max-height: 230px;
  overflow-y: auto;
  padding: 2px;
}

.picker-card {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  background: #f8fafc;
}

.picker-card:hover {
  border-color: #a5b4fc;
  background: #fff;
}

.picker-card.active {
  border-color: var(--el-color-primary);
  background: #eef2ff;
  box-shadow: 0 0 0 1px var(--el-color-primary);
}

.picker-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.picker-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 90px;
}

.picker-price {
  font-size: 12.5px;
  font-weight: 700;
  color: #ef4444;
  margin-top: 2px;
}

.picker-check {
  position: absolute;
  top: 4px;
  right: 6px;
  font-size: 12px;
  font-weight: 800;
  color: var(--el-color-primary);
}

.selected-product-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px;
  margin-bottom: 16px;
}

.prod-badge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.prod-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.prod-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.form-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.form-item-row .label {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-regular);
  display: flex;
  align-items: center;
  gap: 6px;
}

.bill-summary {
  background: #f8fafc;
  border-radius: var(--radius-md);
  padding: 14px;
  margin: 14px 0;
}

.bill-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 0;
}

.bill-row.cut {
  color: #10b981;
}

.bill-row.grand-total {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.price-text {
  font-size: 20px;
  font-weight: 800;
  color: #ef4444;
}

.checkout-submit-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
}

.last-order-banner {
  margin-top: 14px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-md);
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  color: #065f46;
}

/* 右侧流程图解 */
.seata-diagram {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 14px 0;
}

.diagram-step {
  display: flex;
  gap: 12px;
}

.step-badge {
  font-size: 11px;
  font-weight: 700;
  color: #4f46e5;
  background: #eef2ff;
  border-radius: var(--radius-sm);
  padding: 4px 8px;
  align-self: flex-start;
  white-space: nowrap;
}

.step-badge.phase2 {
  color: #059669;
  background: #ecfdf5;
}

.step-box {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 10px 14px;
}

.highlight-box {
  border-left: 3px solid #6366f1;
}

.commit-box {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-left: 3px solid #10b981;
}

.step-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.step-role {
  font-family: monospace;
  font-size: 11px;
  color: #6366f1;
  margin: 2px 0;
}

.step-desc {
  font-size: 11.5px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.diagram-connector {
  text-align: center;
  font-size: 16px;
  color: #94a3b8;
  font-weight: 700;
  margin: -4px 0;
}

.faq-box {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: var(--radius-md);
  padding: 14px;
  margin-top: 16px;
}

.faq-title {
  font-size: 13px;
  font-weight: 700;
  color: #92400e;
  margin-bottom: 6px;
}

.faq-desc {
  font-size: 12px;
  color: #78350f;
  line-height: 1.6;
}
</style>
