<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Check, Box } from '@element-plus/icons-vue'
import { listProducts, getProductDetail } from '@/api/product'
import { createOrder } from '@/api/order'
import type { Product, ProductDetail } from '@/types'

/**
 * 下单页：选择商品 → 查看库存/价格 → 填写数量 → 提交订单。
 * 下单走 POST /api/order/create（Seata 分布式事务：查商品 → 扣库存 → 落订单），
 * 成功后提示订单 ID 并刷新库存。
 */
const products = ref<Product[]>([])
const detail = ref<ProductDetail | null>(null)
const loadingDetail = ref(false)
const submitting = ref(false)

const form = reactive({
  productId: '' as string,
  count: 1,
})

/** 加载商品列表，用于下拉选择 */
async function loadProducts() {
  products.value = await listProducts()
}

/** 选中商品后，拉取它的详情（含库存和价格） */
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

/** 提交下单 */
async function submit() {
  if (!form.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  submitting.value = true
  try {
    const orderId = await createOrder({ productId: form.productId, count: form.count })
    ElMessage.success(`下单成功！订单 ID：${orderId}`)
    // 下单会扣库存，重新拉一次详情让库存数字更新
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

// 监听商品选择变化，自动刷新详情
watch(() => form.productId, loadDetail)

onMounted(loadProducts)
</script>

<template>
  <div class="page">
    <div class="card form-card">
      <div class="card-head">
        <div class="head-icon"><el-icon><ShoppingCart /></el-icon></div>
        <div>
          <div class="card-title">创建订单</div>
          <div class="card-sub">选择商品并填写数量，下单会走 Seata 分布式事务（查商品 → 扣库存 → 落订单）</div>
        </div>
      </div>

      <el-form label-width="90px" style="max-width: 560px; margin-top: 24px">
        <el-form-item label="选择商品">
          <el-select
            v-model="form.productId"
            placeholder="请选择要购买的商品"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in products"
              :key="p.id"
              :label="`${p.name}（￥${Number(p.price).toFixed(2)}）`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="购买数量">
          <el-input-number
            v-model="form.count"
            :min="1"
            :max="999"
            :disabled="!detail"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <!-- 选中商品后的信息面板 -->
      <div v-if="detail" v-loading="loadingDetail" class="preview">
        <div class="preview-top">
          <div class="preview-avatar">{{ detail.product.name?.charAt(0) ?? '?' }}</div>
          <div class="preview-info">
            <div class="preview-name">{{ detail.product.name }}</div>
            <div class="preview-price">￥{{ Number(detail.product.price).toFixed(2) }}</div>
          </div>
          <div class="preview-stock">
            <el-icon><Box /></el-icon>
            <span v-if="detail.stock">库存 {{ detail.stock.quantity }} 件</span>
            <span v-else class="no-stock">未配库存</span>
          </div>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="submitting"
          :disabled="!detail.stock || detail.stock.quantity < 1"
          :icon="Check"
          @click="submit"
        >
          提交订单
        </el-button>

        <div v-if="!detail.stock" class="hint">该商品还没有库存，请先到「库存配置」页配置</div>
        <div v-else-if="detail.stock.quantity < 1" class="hint">库存不足，无法下单</div>
      </div>

      <el-empty
        v-else
        description="请先在上方选择一个商品"
        :image-size="90"
      />
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  justify-content: center;
}
.form-card {
  width: 100%;
  max-width: 640px;
}
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 28px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.head-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.card-sub {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
  max-width: 480px;
}

/* 选中商品预览 */
.preview {
  margin-top: 8px;
  padding: 20px;
  border: 1px solid #eef0f5;
  border-radius: var(--radius-md);
  background: #fafbff;
}
.preview-top {
  display: flex;
  align-items: center;
  gap: 14px;
}
.preview-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}
.preview-info {
  flex: 1;
}
.preview-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.preview-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
  margin-top: 4px;
}
.preview-stock {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
  padding: 6px 12px;
  background: #fff;
  border-radius: 20px;
}
.preview-stock .el-icon {
  color: var(--el-color-primary);
}
.no-stock {
  color: #ef4444;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  margin-top: 18px;
}
.hint {
  margin-top: 12px;
  font-size: 13px;
  color: #e6a23c;
  text-align: center;
}
</style>
