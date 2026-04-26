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
        path: 'mrp-compare',
        name: 'MrpCompare',
        component: () => import('@/views/MrpCompareView.vue')
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
        path: 'capacity-assessment-monthly',
        name: 'CapacityAssessmentMonthly',
        component: () => import('@/views/CapacityAssessmentMonthlyView.vue')
      },
      {
        path: 'capacity-realtime',
        name: 'CapacityAssessmentRealtime',
        component: () => import('@/views/CapacityAssessmentRealtimeView.vue')
      },
      {
        path: 'capacity-realtime-monthly',
        name: 'CapacityAssessmentRealtimeMonthly',
        component: () => import('@/views/CapacityAssessmentRealtimeMonthlyView.vue')
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UsersView.vue'),
        meta: { allowedRoles: ['ADMIN'] }
      },
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('@/views/AiConfigView.vue'),
        meta: { allowedRoles: ['ADMIN'] }
      },
      {
        path: 'fusion-workbench',
        name: 'FusionWorkbench',
        component: () => import('@/views/FusionWorkbenchView.vue')
      },
      {
        path: 'ct-line',
        name: 'CtLine',
        component: () => import('@/views/CtLineView.vue')
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
    return
  }

  const allowedRoles = to.meta?.allowedRoles
  if (token && Array.isArray(allowedRoles) && allowedRoles.length > 0) {
    let roleCodes = []
    try {
      roleCodes = JSON.parse(localStorage.getItem('capics_role_codes') || '[]')
    } catch (e) {
      roleCodes = []
    }
    const normalized = roleCodes.map((v) => String(v || '').trim().toUpperCase())
    const matched = allowedRoles.some((role) => normalized.includes(String(role || '').trim().toUpperCase()))
    if (!matched) {
      next('/')
      return
    }
  }

  if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
