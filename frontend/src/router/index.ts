import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { isLoggedIn } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '经营驾驶舱' }
        },
        {
          path: 'products',
          name: 'ProductManage',
          component: () => import('@/views/ProductView.vue'),
          meta: { title: '商品与价格管理' }
        },
        {
          path: 'categories',
          name: 'CategoryManage',
          component: () => import('@/views/CategoryView.vue'),
          meta: { title: '商品分类' }
        },
        {
          path: 'suppliers',
          name: 'SupplierManage',
          component: () => import('@/views/SupplierView.vue'),
          meta: { title: '供应商管理' }
        },
        {
          path: 'purchase',
          name: 'PurchaseManage',
          component: () => import('@/views/PurchaseView.vue'),
          meta: { title: '采购管理' }
        },
        {
          path: 'inventory',
          name: 'InventoryManage',
          component: () => import('@/views/InventoryView.vue'),
          meta: { title: '库存管理' }
        },
        {
          path: 'sales',
          name: 'SalesManage',
          component: () => import('@/views/SalesView.vue'),
          meta: { title: '销售与收银' }
        },
        {
          path: 'members',
          name: 'MemberManage',
          component: () => import('@/views/MemberView.vue'),
          meta: { title: '会员管理' }
        },
        {
          path: 'promotions',
          name: 'PromotionManage',
          component: () => import('@/views/PromotionView.vue'),
          meta: { title: '促销管理' }
        },
        {
          path: 'stocktake',
          name: 'StocktakeManage',
          component: () => import('@/views/StocktakeView.vue'),
          meta: { title: '盘点与损耗' }
        },
        {
          path: 'analytics',
          name: 'Analytics',
          component: () => import('@/views/AnalyticsView.vue'),
          meta: { title: '经营分析' }
        },
        {
          path: 'agent',
          name: 'AgentCenter',
          component: () => import('@/views/AgentView.vue'),
          meta: { title: 'AI 智能中心' }
        },
        {
          path: 'system',
          name: 'SystemManage',
          component: () => import('@/views/SystemView.vue'),
          meta: { title: '系统管理' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// 登录守卫
router.beforeEach((to) => {
  if (!to.meta.public && !isLoggedIn()) {
    return { path: '/login' }
  }
  if (to.path === '/login' && isLoggedIn()) {
    return { path: '/dashboard' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 中小型超市运营管理系统`
    : '中小型超市运营管理系统'
})

export default router
