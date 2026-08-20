import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表：每个页面一条记录，path 对应侧边栏菜单。
 * 组件用动态 import（懒加载）：首次进入页面才加载对应代码，加快首屏速度。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },
    {
      path: '/products',
      name: 'ProductList',
      component: () => import('@/views/ProductList.vue'),
      meta: { title: '商品列表' },
    },
    {
      path: '/products/:id',
      name: 'ProductDetail',
      component: () => import('@/views/ProductDetail.vue'),
      meta: { title: '商品详情' },
    },
    {
      path: '/order',
      name: 'PlaceOrder',
      component: () => import('@/views/PlaceOrder.vue'),
      meta: { title: '下单' },
    },
    {
      path: '/stock',
      name: 'StockManage',
      component: () => import('@/views/StockManage.vue'),
      meta: { title: '配库存' },
    },
    {
      path: '/orders',
      name: 'OrderList',
      component: () => import('@/views/OrderList.vue'),
      meta: { title: '订单列表' },
    },
  ],
})

export default router
