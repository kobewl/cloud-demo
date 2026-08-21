<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Share,
  CircleCheckFilled,
  Document,
  DocumentCopy,
  Coin,
  Goods,
  Cpu,
} from '@element-plus/icons-vue'
import type { Order } from '@/types'

const props = defineProps<{
  visible: boolean
  order: Order | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

// 模拟或生成的 Seata 全局事务 XID (基于 IP:Port:TransactionID)
const xid = computed(() => {
  if (!props.order?.id) return ''
  const hash = props.order.id.slice(-6)
  return `192.168.1.100:8091:782194301${hash}`
})

function copyJson() {
  if (!props.order) return
  navigator.clipboard.writeText(JSON.stringify(props.order, null, 2))
  ElMessage.success('订单 JSON 数据已复制到剪贴板')
}
</script>

<template>
  <el-drawer
    :model-value="props.visible"
    title="Seata 分布式事务执行链路追踪 (Global Transaction Trace)"
    size="520px"
    direction="rtl"
    class="seata-drawer"
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <div v-if="order" class="drawer-container">
      <!-- 头部卡片 -->
      <div class="xid-card">
        <div class="xid-header">
          <div class="xid-icon">
            <el-icon><Share /></el-icon>
          </div>
          <div class="xid-info">
            <div class="xid-label">Seata 全局事务 ID (XID)</div>
            <div class="xid-value" :title="xid">{{ xid }}</div>
          </div>
          <el-tag type="success" effect="dark" size="small" round>AT 模式 2PC</el-tag>
        </div>
        <div class="xid-meta">
          <span>事务发起者 (TM): <strong>service-order</strong></span>
          <span class="dot-sep">·</span>
          <span>事务协调器 (TC): <strong>Seata Server:8091</strong></span>
        </div>
      </div>

      <!-- 订单基本属性 -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Document /></el-icon>
          <span>业务订单详情</span>
        </div>
        <el-descriptions :column="2" border size="small" class="order-desc">
          <el-descriptions-item label="订单 ID" :span="2">
            <span class="mono-text">{{ order.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ order.productName }}</el-descriptions-item>
          <el-descriptions-item label="购买数量">{{ order.count }} 件</el-descriptions-item>
          <el-descriptions-item label="商品单价">￥{{ Number(order.price).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="订单总金额">
            <strong class="price-highlight">￥{{ Number(order.totalAmount).toFixed(2) }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ order.createdAt || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 分布式事务全生命周期时间线 -->
      <div class="timeline-section">
        <div class="section-title">
          <el-icon><Cpu /></el-icon>
          <span>2PC 分布式事务执行生命周期</span>
        </div>

        <el-timeline class="seata-timeline">
          <!-- Step 1: TM 开启全局事务 -->
          <el-timeline-item
            timestamp="Phase 1 - 0ms"
            placement="top"
            type="primary"
            :icon="Share"
            size="large"
          >
            <div class="timeline-card">
              <div class="timeline-card-title">
                <span>TM 开启全局事务</span>
                <el-tag size="small" type="primary">@GlobalTransactional</el-tag>
              </div>
              <div class="timeline-card-desc">
                <code>service-order</code> 向 Seata TC (事务协调器) 发起 <code>GlobalBegin</code> 请求，生成全局唯一的 XID 并在服务间 Feign 调用链路中隐式传递。
              </div>
            </div>
          </el-timeline-item>

          <!-- Step 2: 分支事务 1 - 扣减库存 -->
          <el-timeline-item
            timestamp="Phase 1 - 12ms"
            placement="top"
            type="success"
            :icon="Goods"
            size="large"
          >
            <div class="timeline-card">
              <div class="timeline-card-title">
                <span>Branch 1: 注册分支事务 & 扣减库存</span>
                <el-tag size="small" type="success">service-stock</el-tag>
              </div>
              <div class="timeline-card-desc">
                <code>service-stock</code> 作为 Resource Manager (RM) 加入该 XID：
                <ul>
                  <li>解析 SQL 提取 BEFORE-IMAGE (扣减前库存)</li>
                  <li>执行业务 SQL 扣除库存 <strong>-{{ order.count }}</strong></li>
                  <li>提取 AFTER-IMAGE 并写入 <code>undo_log</code> 回滚日志表</li>
                  <li>成功提交本地事务并释放本地锁，不长时间阻塞数据库连接</li>
                </ul>
              </div>
            </div>
          </el-timeline-item>

          <!-- Step 3: 分支事务 2 - 落订单 -->
          <el-timeline-item
            timestamp="Phase 1 - 25ms"
            placement="top"
            type="success"
            :icon="Coin"
            size="large"
          >
            <div class="timeline-card">
              <div class="timeline-card-title">
                <span>Branch 2: 注册分支事务 & 生成订单</span>
                <el-tag size="small" type="success">service-order</el-tag>
              </div>
              <div class="timeline-card-desc">
                <code>service-order</code> (RM) 执行 <code>INSERT INTO `order`</code>：
                <ul>
                  <li>生成雪花算法订单 ID (<code>{{ order.id }}</code>)</li>
                  <li>记录 AFTER-IMAGE 到本地 <code>undo_log</code> 表</li>
                  <li>向 TC 报告分支事务 Phase 1 准备就绪</li>
                </ul>
              </div>
            </div>
          </el-timeline-item>

          <!-- Step 4: Phase 2 全局提交 -->
          <el-timeline-item
            timestamp="Phase 2 - 32ms"
            placement="top"
            type="success"
            :icon="CircleCheckFilled"
            size="large"
          >
            <div class="timeline-card commit-card">
              <div class="timeline-card-title">
                <span class="commit-text">Phase 2: TC 异步全局提交 (Global Commit)</span>
                <el-tag size="small" effect="dark" type="success">事务成功完成</el-tag>
              </div>
              <div class="timeline-card-desc">
                全链路无异常抛出，TM 请求 TC 提交全局事务。TC 异步通知所有 RM 分支清理各自数据库中的 <code>undo_log</code>，高性能零阻塞完成 2PC 提交！
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button :icon="DocumentCopy" @click="copyJson">复制订单数据</el-button>
        <el-button type="primary" @click="emit('update:visible', false)">关闭</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.xid-card {
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  color: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.2);
}

.xid-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.xid-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.xid-info {
  flex: 1;
  min-width: 0;
}

.xid-label {
  font-size: 12px;
  color: #94a3b8;
}

.xid-value {
  font-family: monospace;
  font-size: 13px;
  font-weight: 600;
  color: #a5b4fc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xid-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  color: #cbd5e1;
}

.xid-meta strong {
  color: #38bdf8;
}

.dot-sep {
  color: #64748b;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.section-title .el-icon {
  color: var(--el-color-primary);
}

.order-desc {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.mono-text {
  font-family: monospace;
  font-size: 12px;
  color: #475569;
}

.price-highlight {
  color: #ef4444;
  font-size: 15px;
}

.timeline-section {
  margin-top: 6px;
}

.seata-timeline {
  padding-left: 4px;
}

.timeline-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 4px;
}

.commit-card {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.timeline-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 13.5px;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.commit-text {
  color: #065f46;
}

.timeline-card-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.timeline-card-desc ul {
  margin: 4px 0 0;
  padding-left: 18px;
}

.timeline-card-desc li {
  margin-bottom: 2px;
}

.timeline-card-desc code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 600;
  font-family: monospace;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
