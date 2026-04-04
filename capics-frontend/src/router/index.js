import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue')
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/ProductsView.vue')
      },
      {
        path: 'mrp',
        name: 'Mrp',
        component: () => import('@/views/MrpView.vue')
      },
      {
        path: 'routing',
        name: 'Routing',
        component: () => import('@/views/RoutingView.vue')
      },
      {
        path: 'lines',
        name: 'Lines',
        component: () => import('@/views/LinesView.vue')
      },
      {
        path: 'product-line',
        name: 'ProductLine',
        component: () => import('@/views/ProductLineView.vue')
      },
      {
        path: 'capacity-assessment',
        name: 'CapacityAssessment',
        component: () => import('@/views/CapacityAssessmentView.vue')
      },
      {
        path: 'capacity-realtime',
        name: 'CapacityAssessmentRealtime',
        component: () => import('@/views/CapacityAssessmentRealtimeView.vue')
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UsersView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('capics_token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
