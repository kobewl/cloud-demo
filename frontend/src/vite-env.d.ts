/// <reference types="vite/client" />

// 告诉 TypeScript：`.vue` 文件是一个 Vue 组件。没有这个声明，import .vue 会报"找不到模块"。
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
