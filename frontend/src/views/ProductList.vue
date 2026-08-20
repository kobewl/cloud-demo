<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Goods, Search, Refresh, Plus, View, Box, Delete } from '@element-plus/icons-vue'
import { listProducts, createProduct, deleteProduct } from '@/api/product'
import type { Product } from '@/types'

/**
 * 商品列表页：展示所有商品，支持新增、删除，以及跳转到详情/配库存。
 * 数据来自商品服务（走网关 /api/product/list）。
 */
const router = useRouter()

const loading = ref(false)
const products = ref<Product[]>([])
const keyword = ref('')

// 顶部统计：商品总数
const totalCount = computed(() => products.value.length)

// 搜索过滤：按名称模糊匹配
const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return products.value
  return products.value.filter((p) => p.name.toLowerCase().includes(kw))
})

// 新增商品的弹窗开关 + 表单数据
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  name: '',
  price: undefined as number | undefined,
  description: '',
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

/** 加载商品列表 */
async function loadProducts() {
  loading.value = true
  try {
    products.value = await listProducts()
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗（先清空上次残留） */
function openCreate() {
  form.name = ''
  form.price = undefined
  form.description = ''
  dialogVisible.value = true
}

/** 提交新增商品 */
async function submitCreate() {
  await formRef.value.validate()
  await createProduct({
    name: form.name,
    price: form.price!,
    description: form.description,
  })
  ElMessage.success('商品创建成功')
  dialogVisible.value = false
  await loadProducts()
}

/** 删除商品（二次确认） */
async function handleDelete(row: Product) {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  await loadProducts()
}

onMounted(loadProducts)
</script>

<template>
  <div class="page">
    <!-- 顶部统计卡片 -->
    <div class="stat-bar">
      <div class="stat-card">
        <div class="stat-icon goods"><el-icon><Goods /></el-icon></div>
        <div>
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">商品总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon search"><el-icon><Search /></el-icon></div>
        <div>
          <div class="stat-value">{{ filteredProducts.length }}</div>
          <div class="stat-label">筛选结果</div>
        </div>
      </div>
    </div>

    <!-- 主卡片：工具条 + 表格 -->
    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称"
          clearable
          class="search-input"
          :prefix-icon="Search"
        />
        <div class="spacer" />
        <el-button :icon="Refresh" @click="loadProducts">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      </div>

      <el-table :data="filteredProducts" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" min-width="180" show-overflow-tooltip />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="goods-cell">
              <div class="goods-avatar">{{ row.name?.charAt(0) ?? '?' }}</div>
              <div>
                <div class="goods-name">{{ row.name }}</div>
                <div class="goods-desc">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <span class="price">￥{{ Number(row.price).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="router.push(`/products/${row.id}`)">
              详情/下单
            </el-button>
            <el-button link type="warning" :icon="Box" @click="router.push(`/stock?productId=${row.id}`)">
              配库存
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有商品，点击右上角「新增商品」开始" />
        </template>
      </el-table>
    </div>

    <!-- 新增商品弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增商品" width="480px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计卡片 */
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
  min-width: 200px;
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
.stat-icon.goods {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.stat-icon.search {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
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

/* 主卡片 */
.card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.search-input {
  width: 260px;
}
.spacer {
  flex: 1;
}

/* 商品单元格 */
.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.goods-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eef1ff, #f5f3ff);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}
.goods-name {
  font-weight: 600;
  color: #1f2937;
}
.goods-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.price {
  color: #ef4444;
  font-weight: 600;
}
</style>
