<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, Check } from '@element-plus/icons-vue'
import { createStock } from '@/api/stock'
import { listProducts } from '@/api/product'
import type { Product } from '@/types'

/**
 * 配库存页：给商品初始化库存（POST /api/stock）。
 * 后端约束：一个商品只能有一条库存记录，重复配会返回 STOCK_EXISTS 明确提示。
 */
const route = useRoute()

const submitting = ref(false)
const products = ref<Product[]>([])

const form = reactive({
  productId: '' as string,
  quantity: 1,
})

/** 加载商品列表，用于下拉选择 */
async function loadProducts() {
  products.value = await listProducts()
}

/** 提交配库存 */
async function submit() {
  if (!form.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  submitting.value = true
  try {
    await createStock({ productId: form.productId, quantity: form.quantity })
    ElMessage.success('库存配置成功')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadProducts()
  // 支持从商品列表页跳转过来时自动带上商品 ID（/stock?productId=xxx）
  const pid = route.query.productId as string | undefined
  if (pid) {
    form.productId = pid
  }
})
</script>

<template>
  <div class="page">
    <div class="card form-card">
      <div class="card-head">
        <div class="head-icon"><el-icon><Box /></el-icon></div>
        <div>
          <div class="card-title">给商品配置初始库存</div>
          <div class="card-sub">选择商品并填写数量，为它建立一条库存记录</div>
        </div>
      </div>

      <el-form label-width="90px" style="max-width: 520px; margin-top: 24px">
        <el-form-item label="选择商品">
          <el-select
            v-model="form.productId"
            placeholder="请选择商品"
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
        <el-form-item label="库存数量">
          <el-input-number v-model="form.quantity" :min="1" :max="99999" style="width: 100%" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Check" :loading="submitting" @click="submit">
            确认配置
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="tip"
        title="一个商品只能配一条库存记录。重复配置同一商品，后端会返回「该商品已有库存记录」的明确提示。"
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
}
.tip {
  margin-top: 8px;
  border-radius: 10px;
}
</style>
