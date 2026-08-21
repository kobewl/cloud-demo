<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Box,
  Check,
  Plus,
  Refresh,
  Search,
  TrendCharts,
  WarningFilled,
  Document,
} from '@element-plus/icons-vue'
import { createStock, getStock, addStock } from '@/api/stock'
import { listProducts } from '@/api/product'
import type { Product, Stock } from '@/types'

const route = useRoute()

const loading = ref(false)
const products = ref<Product[]>([])
const stocks = ref<Record<string, Stock>>({})
const activeTab = ref('overview') // overview | init | logs
const keyword = ref('')

// 配库存（开户）表单
const initForm = reactive({
  productId: '' as string,
  quantity: 50,
})
const initSubmitting = ref(false)

// 自定义补货弹窗
const customAddDialog = ref(false)
const customAddForm = reactive({
  productId: '',
  productName: '',
  quantity: 20,
})
const customAddSubmitting = ref(false)

// 模拟操作流水记录
interface StockLog {
  id: string
  productName: string
  type: 'INIT' | 'ADD' | 'DEDUCT'
  quantity: number
  operator: string
  time: string
}
const stockLogs = ref<StockLog[]>([
  {
    id: 'L1001',
    productName: 'MacBook Pro M3',
    type: 'INIT',
    quantity: 100,
    operator: '系统管理员',
    time: '2026-08-21 10:00:00',
  },
  {
    id: 'L1002',
    productName: 'iPhone 16 Pro',
    type: 'ADD',
    quantity: 50,
    operator: '库存微服务',
    time: '2026-08-21 11:20:15',
  },
  {
    id: 'L1003',
    productName: 'MacBook Pro M3',
    type: 'DEDUCT',
    quantity: -1,
    operator: 'Seata 事务下单',
    time: '2026-08-21 12:45:00',
  },
])

// 过滤后的商品列表
const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return products.value
  return products.value.filter(
    (p) => p.name.toLowerCase().includes(kw) || p.id.toLowerCase().includes(kw)
  )
})

// 统计数据
const totalSkus = computed(() => products.value.length)
const totalStockQuantity = computed(() =>
  Object.values(stocks.value).reduce((sum, s) => sum + (s.quantity || 0), 0)
)
const warningSkus = computed(() => {
  return products.value.filter((p) => {
    const s = stocks.value[p.id]
    return !s || s.quantity <= 5
  }).length
})

/** 加载商品与库存数据 */
async function loadData() {
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

/** 一键极速入库 (+N 件) */
async function handleQuickAdd(productId: string, amount: number) {
  const p = products.value.find((item) => item.id === productId)
  try {
    const s = stocks.value[productId]
    if (!s) {
      // 未开户直接走开户
      await createStock({ productId, quantity: amount })
      ElMessage.success(`商品「${p?.name}」库存开户成功 (+${amount} 件)`)
    } else {
      await addStock(productId, amount)
      ElMessage.success(`商品「${p?.name}」成功入库 +${amount} 件`)
    }

    // 记录流水
    stockLogs.value.unshift({
      id: 'L' + Math.floor(Math.random() * 9000 + 1000),
      productName: p?.name || '未知商品',
      type: s ? 'ADD' : 'INIT',
      quantity: amount,
      operator: '控制台手动入库',
      time: new Date().toLocaleString(),
    })

    const updated = await getStock(productId, true)
    stocks.value[productId] = updated
  } catch {
    ElMessage.error('入库操作失败')
  }
}

/** 打开自定义入库弹窗 */
function openCustomAdd(p: Product) {
  customAddForm.productId = p.id
  customAddForm.productName = p.name
  customAddForm.quantity = 20
  customAddDialog.value = true
}

/** 提交自定义入库 */
async function submitCustomAdd() {
  customAddSubmitting.value = true
  try {
    await handleQuickAdd(customAddForm.productId, customAddForm.quantity)
    customAddDialog.value = false
  } finally {
    customAddSubmitting.value = false
  }
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
    ElMessage.success('库存配置开户成功！')

    const p = products.value.find((item) => item.id === initForm.productId)
    stockLogs.value.unshift({
      id: 'L' + Math.floor(Math.random() * 9000 + 1000),
      productName: p?.name || '新商品',
      type: 'INIT',
      quantity: initForm.quantity,
      operator: '配库存开户',
      time: new Date().toLocaleString(),
    })

    await loadData()
    activeTab.value = 'overview'
  } finally {
    initSubmitting.value = false
  }
}

onMounted(async () => {
  await loadData()
  const pid = route.query.productId as string | undefined
  if (pid) {
    initForm.productId = pid
    activeTab.value = 'init'
  }
})
</script>

<template>
  <div class="stock-page" v-loading="loading">
    <!-- 顶部状态卡片 -->
    <div class="stat-banner-grid">
      <div class="stat-item tech-card">
        <div class="stat-icon-wrap total"><el-icon><Box /></el-icon></div>
        <div>
          <div class="stat-num">{{ totalStockQuantity }} <span class="unit">件</span></div>
          <div class="stat-lbl">全仓总可用库存</div>
        </div>
      </div>

      <div class="stat-item tech-card">
        <div class="stat-icon-wrap sku"><el-icon><TrendCharts /></el-icon></div>
        <div>
          <div class="stat-num">{{ totalSkus }} <span class="unit">款</span></div>
          <div class="stat-lbl">纳管商品 SKU 总数</div>
        </div>
      </div>

      <div class="stat-item tech-card">
        <div class="stat-icon-wrap warn"><el-icon><WarningFilled /></el-icon></div>
        <div>
          <div class="stat-num text-warn">{{ warningSkus }} <span class="unit">款</span></div>
          <div class="stat-lbl">库存紧张/需补货</div>
        </div>
      </div>
    </div>

    <!-- 主面板与页签 -->
    <div class="main-panel tech-card">
      <el-tabs v-model="activeTab" class="stock-tabs">
        <!-- ===== 页签 1：库存总览与极速补货 ===== -->
        <el-tab-pane label="📊 实时库存大盘与快捷入库" name="overview">
          <div class="tab-toolbar">
            <el-input
              v-model="keyword"
              placeholder="搜索商品名称 / ID"
              clearable
              class="search-box"
              :prefix-icon="Search"
            />
            <div class="spacer" />
            <el-button :icon="Refresh" @click="loadData">刷新库存数据</el-button>
            <el-button type="primary" :icon="Plus" @click="activeTab = 'init'">
              为新商品配库存
            </el-button>
          </div>

          <el-table :data="filteredProducts" stripe style="width: 100%">
            <el-table-column prop="id" label="商品 ID" min-width="180">
              <template #default="{ row }">
                <span class="mono">{{ row.id }}</span>
              </template>
            </el-table-column>

            <el-table-column label="商品名称" min-width="180">
              <template #default="{ row }">
                <div class="product-cell">
                  <div class="p-avatar">{{ row.name.charAt(0) }}</div>
                  <div>
                    <div class="p-name">{{ row.name }}</div>
                    <div class="p-price">￥{{ Number(row.price).toFixed(2) }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="当前可用库存" width="180">
              <template #default="{ row }">
                <div v-if="stocks[row.id]" class="stock-val-wrap">
                  <span
                    class="stock-big-num"
                    :class="{
                      adequate: stocks[row.id].quantity > 5,
                      low: stocks[row.id].quantity > 0 && stocks[row.id].quantity <= 5,
                      empty: stocks[row.id].quantity === 0,
                    }"
                  >
                    {{ stocks[row.id].quantity }}
                  </span>
                  <span class="unit">件</span>
                  <el-tag
                    v-if="stocks[row.id].quantity > 5"
                    type="success"
                    size="small"
                    round
                  >
                    充足
                  </el-tag>
                  <el-tag
                    v-else-if="stocks[row.id].quantity > 0"
                    type="warning"
                    size="small"
                    round
                  >
                    紧张
                  </el-tag>
                  <el-tag v-else type="danger" size="small" round>缺货</el-tag>
                </div>
                <div v-else>
                  <el-tag type="info" size="small">未初始化库存</el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="库存健康水位" min-width="160">
              <template #default="{ row }">
                <el-progress
                  :percentage="Math.min(100, (stocks[row.id]?.quantity ?? 0) * 5)"
                  :status="
                    !stocks[row.id] || stocks[row.id].quantity === 0
                      ? 'exception'
                      : stocks[row.id].quantity <= 5
                      ? 'warning'
                      : 'success'
                  "
                  :stroke-width="6"
                />
              </template>
            </el-table-column>

            <el-table-column label="一键极速入库" width="260" fixed="right">
              <template #default="{ row }">
                <div class="quick-btn-group">
                  <el-button size="small" @click="handleQuickAdd(row.id, 5)">+5</el-button>
                  <el-button size="small" type="primary" plain @click="handleQuickAdd(row.id, 10)">
                    +10
                  </el-button>
                  <el-button size="small" type="primary" plain @click="handleQuickAdd(row.id, 50)">
                    +50
                  </el-button>
                  <el-button size="small" link type="primary" @click="openCustomAdd(row)">
                    自定义
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无商品数据" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- ===== 页签 2：配库存（开户初始化） ===== -->
        <el-tab-pane label="➕ 新商品配库存 (开户)" name="init">
          <div class="form-container">
            <div class="form-desc-alert">
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="配库存（开户说明）"
                description="给「从未有过库存记录」的新商品建立第一条库存账户。一个商品仅需配一次，后续请使用快捷入库进行补货。"
              />
            </div>

            <el-form label-width="100px" class="init-form">
              <el-form-item label="选择目标商品">
                <el-select
                  v-model="initForm.productId"
                  placeholder="请选择商品"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="p in products"
                    :key="p.id"
                    :label="`${p.name}（当前库存: ${stocks[p.id]?.quantity ?? '未配'}）`"
                    :value="p.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="初始库存数量">
                <el-input-number
                  v-model="initForm.quantity"
                  :min="1"
                  :max="99999"
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :icon="Check"
                  :loading="initSubmitting"
                  @click="submitInit"
                >
                  确认初始化配置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ===== 页签 3：出入库操作流水日志 ===== -->
        <el-tab-pane label="📜 出入库与事务流水日志" name="logs">
          <div class="logs-container">
            <el-table :data="stockLogs" stripe style="width: 100%">
              <el-table-column prop="id" label="流水号" width="120" />
              <el-table-column prop="productName" label="关联商品" min-width="160" />
              <el-table-column label="操作类型" width="130">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.type === 'INIT'"
                    type="primary"
                    effect="light"
                  >
                    开户初始化
                  </el-tag>
                  <el-tag
                    v-else-if="row.type === 'ADD'"
                    type="success"
                    effect="light"
                  >
                    补货入库
                  </el-tag>
                  <el-tag v-else type="danger" effect="light">Seata 下单扣减</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="变动数量" width="130">
                <template #default="{ row }">
                  <strong :style="{ color: row.quantity > 0 ? '#10b981' : '#ef4444' }">
                    {{ row.quantity > 0 ? `+${row.quantity}` : row.quantity }} 件
                  </strong>
                </template>
              </el-table-column>
              <el-table-column prop="operator" label="操作人 / 发起方" width="160" />
              <el-table-column prop="time" label="记录时间" min-width="180" />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 自定义补货对话框 -->
    <el-dialog v-model="customAddDialog" title="自定义快捷入库" width="440px" align-center>
      <el-form label-width="90px">
        <el-form-item label="商品名称">
          <strong>{{ customAddForm.productName }}</strong>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number
            v-model="customAddForm.quantity"
            :min="1"
            :max="99999"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="customAddSubmitting" @click="submitCustomAdd">
          确认入库
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stock-page {
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

.stat-icon-wrap.total {
  background: linear-gradient(135deg, #10b981, #059669);
}
.stat-icon-wrap.sku {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.stat-icon-wrap.warn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
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

.text-warn {
  color: #f59e0b !important;
}

/* 主面板 */
.main-panel {
  padding: 20px 24px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.search-box {
  width: 260px;
}

.spacer {
  flex: 1;
}

.mono {
  font-family: monospace;
  font-size: 12px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.p-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.p-name {
  font-weight: 600;
  color: var(--text-primary);
}

.p-price {
  font-size: 12px;
  color: #ef4444;
}

.stock-val-wrap {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stock-big-num {
  font-size: 16px;
  font-weight: 800;
}

.stock-big-num.adequate {
  color: #10b981;
}
.stock-big-num.low {
  color: #f59e0b;
}
.stock-big-num.empty {
  color: #ef4444;
}

.stock-val-wrap .unit {
  font-size: 12px;
  color: var(--text-muted);
}

.quick-btn-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 表单容器 */
.form-container {
  max-width: 580px;
  padding: 16px 0;
}

.form-desc-alert {
  margin-bottom: 20px;
}
</style>
