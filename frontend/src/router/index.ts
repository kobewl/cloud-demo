import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表：配置各个页面与元信息
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { title: '仪表盘概览' },
    },
    {
      path: '/products',
      name: 'ProductList',
      component: () => import('@/views/ProductList.vue'),
      meta: { title: '商品资产管理' },
    },
    {
      path: '/products/:id',
      name: 'ProductDetail',
      component: () => import('@/views/ProductDetail.vue'),
      meta: { title: '商品全景工作台' },
    },
    {
      path: '/order',
      name: 'PlaceOrder',
      component: () => import('@/views/PlaceOrder.vue'),
      meta: { title: '极速下单工作台' },
    },
    {
      path: '/stock',
      name: 'StockManage',
      component: () => import('@/views/StockManage.vue'),
      meta: { title: '智能库存管控' },
    },
    {
      path: '/orders',
      name: 'OrderList',
      component: () => import('@/views/OrderList.vue'),
      meta: { title: '订单与事务中心' },
    },
    {
      path: '/governance',
      name: 'ServiceGovernance',
      component: () => import('@/views/ServiceGovernance.vue'),
      meta: { title: '微服务治理与拓扑' },
    },
  ],
})

export default router

