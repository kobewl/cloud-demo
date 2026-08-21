<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Goods,
  Search,
  Refresh,
  Plus,
  View,
  Box,
  Delete,
  Grid,
  Menu,
  ShoppingCart,
  Download,
  DocumentCopy,
  TopRight,
} from '@element-plus/icons-vue'
import { listProducts, createProduct, deleteProduct } from '@/api/product'
import { getStock, addStock, createStock } from '@/api/stock'
import type { Product, Stock } from '@/types'

const router = useRouter()

const loading = ref(false)
const products = ref<Product[]>([])
const stocks = ref<Record<string, Stock>>({})
const keyword = ref('')
const stockFilter = ref('all') // all | adequate | low | empty
const viewMode = ref<'grid' | 'table'>('grid')
const selectedProducts = ref<Product[]>([])

// 统计
const totalCount = computed(() => products.value.length)

// 过滤后的商品列表
const filteredProducts = computed(() => {
  let list = products.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (p) =>
        p.name.toLowerCase().includes(kw) ||
        (p.description && p.description.toLowerCase().includes(kw)) ||
        p.id.toLowerCase().includes(kw)
    )
  }

  if (stockFilter.value !== 'all') {
    list = list.filter((p) => {
      const s = stocks.value[p.id]
      const qty = s ? s.quantity : 0
      if (stockFilter.value === 'adequate') return qty > 5
      if (stockFilter.value === 'low') return qty > 0 && qty <= 5
      if (stockFilter.value === 'empty') return !s || qty === 0
      return true
    })
  }

  return list
})

// 新增商品表单
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  name: '',
  price: undefined as number | undefined,
  description: '',
  initialStock: 10,
  themeColor: 'indigo',
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
}

// 渐变主题色配置
const colorThemes: Record<string, { bg: string; text: string }> = {
  indigo: { bg: 'linear-gradient(135deg, #6366f1, #8b5cf6)', text: '#fff' },
  cyan: { bg: 'linear-gradient(135deg, #06b6d4, #3b82f6)', text: '#fff' },
  emerald: { bg: 'linear-gradient(135deg, #10b981, #059669)', text: '#fff' },
  amber: { bg: 'linear-gradient(135deg, #f59e0b, #d97706)', text: '#fff' },
  rose: { bg: 'linear-gradient(135deg, #f43f5e, #e11d48)', text: '#fff' },
}

/** 加载商品与对应库存 */
async function loadProducts() {
  loading.value = true
  try {
    products.value = await listProducts()
    const stockMap: Record<string, Stock> = {}
    await Promise.all(
      products.value.map(async (p) => {
        try {
          const s = await getStock(p.id, true)
          if (s) stockMap[p.id] = s
        } catch {
          // ignore
        }
      })
    )
    stocks.value = stockMap
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
function openCreate() {
  form.name = ''
  form.price = undefined
  form.description = ''
  form.initialStock = 10
  form.themeColor = 'indigo'
  dialogVisible.value = true
}

/** 提交新增商品（并自动初始化库存） */
async function submitCreate() {
  await formRef.value.validate()
  loading.value = true
  try {
    await createProduct({
      name: form.name,
      price: form.price!,
      description: form.description,
    })
    ElMessage.success('商品创建成功！')
    dialogVisible.value = false
    await loadProducts()

    // 尝试为最新匹配名字的商品初始化库存
    if (form.initialStock > 0) {
      const newlyCreated = products.value.find((p) => p.name === form.name)
      if (newlyCreated) {
        try {
          await createStock({ productId: newlyCreated.id, quantity: form.initialStock })
          ElMessage.success(`已自动为商品配置初始库存 ${form.initialStock} 件`)
          await loadProducts()
        } catch {
          // ignore if already stocked
        }
      }
    }
  } finally {
    loading.value = false
  }
}

/** 行内快捷补货 (+10) */
async function quickAddStock(productId: string, e?: Event) {
  e?.stopPropagation()
  try {
    const s = stocks.value[productId]
    if (!s) {
      // 尚未配库存，走开户
      await createStock({ productId, quantity: 10 })
      ElMessage.success('库存开户成功 (+10 件)')
    } else {
      await addStock(productId, 10)
      ElMessage.success('库存快捷增加 +10 件')
    }
    const updated = await getStock(productId, true)
    stocks.value[productId] = updated
  } catch {
    ElMessage.error('补货失败，请重试')
  }
}

/** 删除商品 */
async function handleDelete(row: Product, e?: Event) {
  e?.stopPropagation()
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？此操作无法撤销。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  await loadProducts()
}

/** 复制商品 ID */
function copyId(id: string, e?: Event) {
  e?.stopPropagation()
  navigator.clipboard.writeText(id)
  ElMessage.success('商品 ID 已复制')
}

/** 导出 CSV */
function exportCSV() {
  if (products.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const headers = ['商品ID,商品名称,价格,当前库存,描述,创建时间']
  const rows = filteredProducts.value.map((p) => {
    const s = stocks.value[p.id]?.quantity ?? 0
    return `"${p.id}","${p.name}",${p.price},${s},"${p.description || ''}","${p.createdAt || ''}"`
  })
  const csvContent = '\uFEFF' + headers.concat(rows).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `商品列表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('商品数据已成功导出 CSV')
}

function getThemeStyle(name: string) {
  const code = name.charCodeAt(0) || 0
  const keys = Object.keys(colorThemes)
  const key = keys[code % keys.length]
  return colorThemes[key].bg
}

onMounted(loadProducts)
</script>

<template>
  <div class="product-page" v-loading="loading">
    <!-- 顶部状态栏与操作条 -->
    <div class="toolbar-card tech-card">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称 / ID / 描述"
          clearable
          class="search-input"
          :prefix-icon="Search"
        />

        <el-radio-group v-model="stockFilter" class="stock-filter">
          <el-radio-button label="all">全部商品 ({{ totalCount }})</el-radio-button>
          <el-radio-button label="adequate">库存充足</el-radio-button>
          <el-radio-button label="low">库存紧张</el-radio-button>
          <el-radio-button label="empty">缺货/未配</el-radio-button>
        </el-radio-group>
      </div>

      <div class="toolbar-right">
        <!-- 视图切换 -->
        <el-radio-group v-model="viewMode" size="default">
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon> 卡片
          </el-radio-button>
          <el-radio-button label="table">
            <el-icon><Menu /></el-icon> 表格
          </el-radio-button>
        </el-radio-group>

        <el-button :icon="Download" @click="exportCSV">导出</el-button>
        <el-button :icon="Refresh" @click="loadProducts">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      </div>
    </div>

    <!-- 视图 1：精美卡片网格视图 -->
    <div v-if="viewMode === 'grid'" class="grid-container">
      <div
        v-for="p in filteredProducts"
        :key="p.id"
        class="product-grid-card tech-card tech-card-hover"
        @click="router.push(`/products/${p.id}`)"
      >
        <!-- 卡片顶部 Banner -->
        <div class="card-hero" :style="{ background: getThemeStyle(p.name) }">
          <div class="hero-avatar">{{ p.name.charAt(0) }}</div>
          <div class="hero-badge">
            <el-tag
              v-if="stocks[p.id] && stocks[p.id].quantity > 5"
              type="success"
              effect="dark"
              size="small"
              round
            >
              库存充足 ({{ stocks[p.id].quantity }})
            </el-tag>
            <el-tag
              v-else-if="stocks[p.id] && stocks[p.id].quantity > 0"
              type="warning"
              effect="dark"
              size="small"
              round
            >
              紧张 (仅剩 {{ stocks[p.id].quantity }})
            </el-tag>
            <el-tag v-else type="danger" effect="dark" size="small" round>缺货/未配</el-tag>
          </div>
        </div>

        <!-- 卡片内容 -->
        <div class="card-body">
          <div class="product-title-row">
            <h3 class="product-name" :title="p.name">{{ p.name }}</h3>
            <div class="product-price">
              <span class="symbol">￥</span>{{ Number(p.price).toFixed(2) }}
            </div>
          </div>

          <p class="product-desc" :title="p.description">
            {{ p.description || '暂无商品描述信息' }}
          </p>

          <div class="stock-meter">
            <div class="meter-info">
              <span>库存水位</span>
              <span class="meter-val">{{ stocks[p.id]?.quantity ?? 0 }} 件</span>
            </div>
            <el-progress
              :percentage="Math.min(100, (stocks[p.id]?.quantity ?? 0) * 5)"
              :status="
                !stocks[p.id] || stocks[p.id].quantity === 0
                  ? 'exception'
                  : stocks[p.id].quantity <= 5
                  ? 'warning'
                  : 'success'
              "
              :stroke-width="6"
              :show-text="false"
            />
          </div>

          <div class="card-id-row">
            <span class="id-text">ID: {{ p.id.slice(0, 12) }}...</span>
            <el-button link type="primary" size="small" :icon="DocumentCopy" @click="copyId(p.id, $event)">
              复制
            </el-button>
          </div>
        </div>

        <!-- 悬浮操作栏 -->
        <div class="card-footer" @click.stop>
          <el-button
            type="primary"
            size="small"
            :icon="ShoppingCart"
            @click="router.push(`/products/${p.id}`)"
          >
            详情/下单
          </el-button>
          <el-button size="small" :icon="Box" @click="quickAddStock(p.id, $event)">
            +10 补货
          </el-button>
          <el-button type="danger" plain size="small" :icon="Delete" @click="handleDelete(p, $event)" />
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredProducts.length === 0" class="empty-wrap tech-card">
        <el-empty description="没有匹配的商品，点击上方「新增商品」或重置搜索条件" />
      </div>
    </div>

    <!-- 视图 2：高级数据表格视图 -->
    <div v-else class="table-container tech-card">
      <el-table :data="filteredProducts" stripe style="width: 100%">
        <el-table-column prop="id" label="商品 ID" min-width="180">
          <template #default="{ row }">
            <div class="id-cell">
              <span class="mono">{{ row.id }}</span>
              <el-button link type="primary" size="small" :icon="DocumentCopy" @click="copyId(row.id)">
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商品基本信息" min-width="220">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="avatar-cell" :style="{ background: getThemeStyle(row.name) }">
                {{ row.name.charAt(0) }}
              </div>
              <div class="name-cell">
                <div class="name">{{ row.name }}</div>
                <div class="desc">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="单价" width="130" sortable prop="price">
          <template #default="{ row }">
            <strong class="table-price">￥{{ Number(row.price).toFixed(2) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="实时库存" width="160">
          <template #default="{ row }">
            <div class="table-stock-cell">
              <template v-if="stocks[row.id]">
                <el-tag
                  :type="stocks[row.id].quantity > 5 ? 'success' : stocks[row.id].quantity > 0 ? 'warning' : 'danger'"
                  size="small"
                >
                  {{ stocks[row.id].quantity }} 件
                </el-tag>
                <el-button link type="primary" size="small" @click="quickAddStock(row.id)">
                  +10
                </el-button>
              </template>
              <template v-else>
                <el-tag type="info" size="small">未配库存</el-tag>
                <el-button link type="primary" size="small" @click="quickAddStock(row.id)">
                  开户
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" min-width="160" />

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="router.push(`/products/${row.id}`)">
              详情/下单
            </el-button>
            <el-button link type="warning" :icon="Box" @click="router.push(`/stock?productId=${row.id}`)">
              配库存
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无符合条件的商品数据" />
        </template>
      </el-table>
    </div>

    <!-- 新增商品对话框 -->
    <el-dialog v-model="dialogVisible" title="新增商品资产" width="500px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="85px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：MacBook Pro M3 Max" />
        </el-form-item>

        <el-form-item label="商品单价" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :precision="2"
            :step="10"
            style="width: 100%"
            placeholder="请输入单价"
          />
        </el-form-item>

        <el-form-item label="初始库存">
          <el-input-number
            v-model="form.initialStock"
            :min="0"
            :max="99999"
            style="width: 100%"
          />
          <div class="form-tip">创建成功后将自动向库存服务 (service-stock) 初始化该库存</div>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="可选：详细的产品特性与规格说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">立即发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar-card {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 卡片网格视图 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.empty-wrap {
  grid-column: 1 / -1;
  padding: 40px;
}

.product-grid-card {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  overflow: hidden;
}

.card-hero {
  height: 90px;
  padding: 14px 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
}

.hero-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-body {
  padding: 16px;
  flex: 1;
}

.product-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.product-name {
  margin: 0;
  font-size: 15.5px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 17px;
  font-weight: 800;
  color: #ef4444;
  white-space: nowrap;
}

.product-price .symbol {
  font-size: 12px;
}

.product-desc {
  margin: 6px 0 14px;
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.5;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.stock-meter {
  background: #f8fafc;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.meter-info {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.meter-val {
  font-weight: 600;
  color: var(--text-primary);
}

.card-id-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
}

.id-text {
  font-family: monospace;
}

.card-footer {
  padding: 12px 16px;
  background: #f8fafc;
  border-top: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

/* 表格视图 */
.table-container {
  padding: 16px;
}

.id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mono {
  font-family: monospace;
  font-size: 12px;
  color: var(--text-regular);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-cell {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.name-cell .name {
  font-weight: 600;
  color: var(--text-primary);
}

.name-cell .desc {
  font-size: 12px;
  color: var(--text-muted);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-price {
  color: #ef4444;
  font-size: 14px;
}

.table-stock-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-tip {
  font-size: 11.5px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
