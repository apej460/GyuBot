import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/signup', name: 'signup', component: () => import('@/views/SignupView.vue') },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: () => import('@/views/MyPageView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/history',
      name: 'chat-history',
      component: () => import('@/views/ChatHistoryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/:id?',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/members',
      name: 'admin-members',
      component: () => import('@/views/AdminMembersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/signup-requests',
      name: 'admin-signup-requests',
      component: () => import('@/views/AdminSignupRequestsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.checkAuth()
  }
  if (to.meta.requiresAuth && !auth.user) {
    return { name: 'login' }
  }
  if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') {
    return { name: 'home' }
  }
  if (to.name === 'login' && auth.user) {
    return { name: 'home' }
  }
})

export default router
