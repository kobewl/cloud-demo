import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import '@/styles/index.css'
import App from './App.vue'
import router from './router'

/**
 * 应用入口：按顺序挂载。
 * 1. Pinia（全局状态）
 * 2. Router（路由）
 * 3. Element Plus（UI 组件库）
 * 4. 全部 Element Plus 图标（全局注册后，模板里可直接用 <el-icon><Goods /></el-icon>）
 */
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 全局注册所有图标组件，省去每个文件单独 import
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
