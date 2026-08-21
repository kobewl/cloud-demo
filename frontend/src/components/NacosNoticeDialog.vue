<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Refresh, Connection, SuccessFilled } from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const shop = useShopStore()
const loading = ref(false)
const copied = ref(false)

async function refreshNotice() {
  loading.value = true
  try {
    await shop.fetchNotice()
    ElMessage.success('已从 Nacos 配置中心拉取最新配置！')
  } catch {
    ElMessage.error('拉取配置失败，请检查服务是否正常运行')
  } finally {
    loading.value = false
  }
}

function copyConfigYaml() {
  const yamlContent = `# Nacos 配置中心 service-product.yaml (Group: cloud-demo)
shop:
  notice: "${shop.notice || '欢迎来到微服务商城！'}"
`
  navigator.clipboard.writeText(yamlContent)
  copied.value = true
  ElMessage.success('配置 YAML 已复制到剪贴板')
  setTimeout(() => {
    copied.value = false
  }, 2000)
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    title="Nacos 配置中心 · 动态配置管理"
    width="580px"
    align-center
    class="nacos-dialog"
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <div class="dialog-content">
      <div class="meta-banner">
        <div class="meta-icon">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="meta-info">
          <div class="meta-title">Data ID: service-product.yaml</div>
          <div class="meta-sub">Group: <span class="group-pill">cloud-demo</span> · @RefreshScope 动态热加载</div>
        </div>
        <el-tag type="success" effect="light" round>
          <span class="pulse-dot pulse-dot-success" style="margin-right: 6px"></span>
          已连接
        </el-tag>
      </div>

      <div class="notice-box">
        <div class="notice-box-head">
          <span class="label">当前生效的公告内容 (shop.notice)：</span>
          <el-button link type="primary" :icon="Refresh" :loading="loading" @click="refreshNotice">
            立即刷新
          </el-button>
        </div>
        <div class="notice-val">
          {{ shop.notice || '（暂无配置或为空）' }}
        </div>
      </div>

      <div class="config-guide">
        <div class="guide-title">
          <span>💡 如何在 Nacos 控制台修改配置并验证动态刷新？</span>
        </div>
        <ol class="guide-steps">
          <li>访问 Nacos 控制台：<el-link type="primary" href="http://localhost:18080" target="_blank">http://localhost:18080</el-link> (账号 nacos / nacos)</li>
          <li>进入 <strong>配置管理 ➔ 配置列表</strong>，切换到 <code>cloud-demo</code> 分组</li>
          <li>找到 <code>service-product.yaml</code> 点击 <strong>编辑</strong></li>
          <li>修改 <code>shop.notice: "新的通知内容"</code> 并点击 <strong>发布</strong></li>
          <li>无需重启任何微服务，点击右上角“刷新”按钮即可见证秒级动态热更新！</li>
        </ol>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button :icon="copied ? SuccessFilled : DocumentCopy" @click="copyConfigYaml">
          {{ copied ? '已复制' : '复制 Nacos 配置片段' }}
        </el-button>
        <el-button type="primary" @click="emit('update:visible', false)">知道了</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.meta-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: var(--radius-md);
  border: 1px solid #e2e8f0;
}

.meta-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.meta-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.meta-sub {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.group-pill {
  font-family: monospace;
  background: #eef2ff;
  color: var(--el-color-primary);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.notice-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px;
}

.notice-box-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.notice-box-head .label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.notice-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 10px 14px;
  background: #f1f5f9;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--el-color-primary);
  word-break: break-all;
}

.config-guide {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: var(--radius-md);
  padding: 14px;
}

.guide-title {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 8px;
}

.guide-steps {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: #78350f;
  line-height: 1.8;
}

.guide-steps code {
  background: #fef3c7;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
