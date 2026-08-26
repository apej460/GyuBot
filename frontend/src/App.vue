<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const showShell = computed(() => route.meta.requiresAuth)

const activeMenu = computed(() => {
  if (route.path.startsWith('/chat/history')) return '/chat/history'
  if (route.path.startsWith('/chat')) return '/chat'
  if (route.path.startsWith('/admin/members')) return '/admin/members'
  if (route.path.startsWith('/admin/signup-requests')) return '/admin/signup-requests'
  return route.path
})

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div v-if="showShell" class="shell">
    <header class="shell-header">
      <div class="shell-header-inner">
        <RouterLink to="/" class="brand">GyuBot</RouterLink>
        <el-menu mode="horizontal" :ellipsis="false" router :default-active="activeMenu" class="shell-menu">
          <el-menu-item index="/chat">AI 질의</el-menu-item>
          <el-menu-item index="/chat/history">질의 이력</el-menu-item>
          <el-menu-item index="/mypage">내 정보</el-menu-item>
          <el-menu-item v-if="auth.user?.role === 'ADMIN'" index="/admin/members">회원 관리</el-menu-item>
          <el-menu-item v-if="auth.user?.role === 'ADMIN'" index="/admin/signup-requests">가입 승인</el-menu-item>
        </el-menu>
        <div class="shell-user">
          <span class="shell-user-email">{{ auth.user?.email }}</span>
          <el-tag v-if="auth.user?.role === 'ADMIN'" type="warning" size="small" round>관리자</el-tag>
          <el-button size="small" text @click="handleLogout">로그아웃</el-button>
        </div>
      </div>
    </header>
    <main class="shell-main">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<style>
body {
  margin: 0;
  background: #f5f7fa;
  color: #1f2329;
}
</style>

<style scoped>
.shell {
  min-height: 100vh;
}
.shell-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 10;
}
.shell-header-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.brand {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  text-decoration: none;
  flex-shrink: 0;
}
.shell-menu {
  flex: 1;
  border-bottom: none;
}
.shell-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.shell-user-email {
  font-size: 13px;
  color: #606266;
}
.shell-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}
</style>
