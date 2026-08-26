import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

function isAdminRole(role) {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}

function landingRouteFor(role) {
  return isAdminRole(role) ? { name: 'admin-dashboard' } : { name: 'chat' }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 인증 (SCR-AUTH-*)
    { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/admin/login', name: 'admin-login', component: () => import('@/views/auth/AdminLoginView.vue') },
    { path: '/reset-password', name: 'reset-password', component: () => import('@/views/auth/ResetPasswordView.vue') },
    {
      path: '/signup/email',
      name: 'signup-email',
      component: () => import('@/views/auth/EmailSignupView.vue'),
    },
    {
      path: '/signup/document',
      name: 'signup-document',
      component: () => import('@/views/auth/DocumentSignupView.vue'),
    },

    // 임직원 (SCR-AI-*, SCR-HISTORY-001, SCR-MY-001)
    {
      path: '/chat/:id?',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/views/HistoryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: () => import('@/views/MyPageView.vue'),
      meta: { requiresAuth: true },
    },

    // 관리자 콘솔 (SCR-ADMIN-*, SCR-DOC-*, SCR-MEMBER-001, SCR-SYS-001)
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/DashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/documents',
      name: 'admin-documents',
      component: () => import('@/views/admin/DocumentsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/documents/new',
      name: 'admin-documents-new',
      component: () => import('@/views/admin/DocumentRegisterView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/members',
      name: 'admin-members',
      component: () => import('@/views/admin/MembersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/managers',
      name: 'admin-managers',
      component: () => import('@/views/admin/ManagersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/system',
      name: 'admin-system',
      component: () => import('@/views/admin/SystemView.vue'),
      meta: { requiresAuth: true, requiresSuperAdmin: true },
    },

    { path: '/', redirect: () => landingRouteFor(useAuthStore().user?.role) },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.checkAuth()
  }
  if (to.meta.requiresAuth && !auth.user) {
    return to.meta.requiresAdmin || to.meta.requiresSuperAdmin ? { name: 'admin-login' } : { name: 'login' }
  }
  if (to.meta.requiresSuperAdmin && auth.user?.role !== 'SUPER_ADMIN') {
    return landingRouteFor(auth.user?.role)
  }
  if (to.meta.requiresAdmin && !isAdminRole(auth.user?.role)) {
    return landingRouteFor(auth.user?.role)
  }
  if ((to.name === 'login' || to.name === 'admin-login') && auth.user) {
    return landingRouteFor(auth.user.role)
  }
})

export default router
