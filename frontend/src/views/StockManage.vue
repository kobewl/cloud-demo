<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, Check, Plus } from '@element-plus/icons-vue'
import { createStock, getStock, addStock } from '@/api/stock'
import { listProducts } from '@/api/product'
import type { Product } from '@/types'

/**
 * 库存管理页：两个功能，对应后端两条不同的路。
 * 1. 配库存（POST /api/stock）：给"从未有过库存"的新商品建立第一条库存记录（开户）。
 *    一个商品只能开一次户，重复配会返回 STOCK_EXISTS 明确提示。
 * 2. 增加库存（POST /api/stock/add/{productId}/{count}）：给已有库存的商品补货（存钱），
 *    可反复操作。
 */
const route = useRoute()

const products = ref<Product[]>([])

// 当前激活的页签
const activeTab = ref('init')

// ---- 配库存（开户）表单 ----
const initForm = reactive({
  productId: '' as string,
  quantity: 1,
})
const initSubmitting = ref(false)

// ---- 增加库存（补货）表单 ----
const addForm = reactive({
  productId: '' as string,
  quantity: 1,
})
const addSubmitting = ref(false)
const currentStock = ref<number | null>(null)
const loadingStock = ref(false)

/** 加载商品列表，用于两个页签的下拉选择 */
async function loadProducts() {
  products.value = await listProducts()
}

/** 提交：配库存（开户） */
async function submitInit() {
  if (!initForm.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  initSubmitting.value = true
  try {
    await createStock({ productId: initForm.productId, quantity: initForm.quantity })
    ElMessage.success('库存配置成功')
  } finally {
    initSubmitting.value = false
  }
}

/** 选中补货商品后，查它当前的库存（silent 模式：未配库存时不弹全局错误） */
async function loadCurrentStock() {
  if (!addForm.productId) {
    currentStock.value = null
    return
  }
  loadingStock.value = true
  try {
    const stock = await getStock(addForm.productId, true)
    currentStock.value = stock.quantity
  } catch {
    // silent 模式已吞掉全局提示，这里只需把当前库存置空，表示"还没有库存记录"
    currentStock.value = null
  } finally {
    loadingStock.value = false
  }
}

/** 提交：增加库存（补货） */
async function submitAdd() {
  if (!addForm.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  addSubmitting.value = true
  try {
    await addStock(addForm.productId, addForm.quantity)
    ElMessage.success('库存增加成功')
    await loadCurrentStock() // 补货后刷新当前库存显示
  } finally {
    addSubmitting.value = false
  }
}

// 监听补货商品切换，自动刷新当前库存
watch(() => addForm.productId, loadCurrentStock)

onMounted(async () => {
  await loadProducts()
  // 支持从商品列表页跳转过来时自动带上商品 ID（/stock?productId=xxx）
  const pid = route.query.productId as string | undefined
  if (pid) {
    initForm.productId = pid
    addForm.productId = pid
    await loadCurrentStock()
  }
})
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="card-head">
        <div class="head-icon"><el-icon><Box /></el-icon></div>
        <div>
          <div class="card-title">库存管理</div>
          <div class="card-sub">新商品先「配库存」开户，已有库存的商品用「增加库存」补货</div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="tabs">
        <!-- ===== 页签 1：配库存（开户） ===== -->
        <el-tab-pane label="配库存" name="init">
          <el-form label-width="90px" style="max-width: 520px; margin-top: 8px">
            <el-form-item label="选择商品">
              <el-select
                v-model="initForm.productId"
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
              <el-input-number v-model="initForm.quantity" :min="1" :max="99999" style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Check" :loading="initSubmitting" @click="submitInit">
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
        </el-tab-pane>

        <!-- ===== 页签 2：增加库存（补货） ===== -->
        <el-tab-pane label="增加库存" name="add">
          <el-form label-width="90px" style="max-width: 520px; margin-top: 8px">
            <el-form-item label="选择商品">
              <el-select
                v-model="addForm.productId"
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

            <!-- 当前库存展示 -->
            <el-form-item label="当前库存">
              <div v-loading="loadingStock" class="stock-display">
                <template v-if="addForm.productId">
                  <span v-if="currentStock !== null" class="stock-num">{{ currentStock }}</span>
                  <el-tag v-else type="warning" size="small">未配库存</el-tag>
                </template>
                <span v-else class="placeholder">请先选择商品</span>
              </div>
            </el-form-item>

            <el-form-item label="增加数量">
              <el-input-number v-model="addForm.quantity" :min="1" :max="99999" style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :icon="Plus"
                :loading="addSubmitting"
                :disabled="currentStock === null"
                @click="submitAdd"
              >
                确认增加
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            type="success"
            :closable="false"
            show-icon
            class="tip"
            title="增加库存可反复操作。只有「已配过库存」的商品才能补货，未配库存的商品请先到「配库存」页签开户。"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  justify-content: center;
}
.card {
  width: 100%;
  max-width: 680px;
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
.tabs {
  margin-top: 16px;
}
.stock-display {
  min-height: 32px;
  display: flex;
  align-items: center;
}
.stock-num {
  font-size: 24px;
  font-weight: 700;
  color: #10b981;
}
.placeholder {
  color: #c0c4cc;
  font-size: 13px;
}
.tip {
  margin-top: 8px;
  border-radius: 10px;
}
</style>
