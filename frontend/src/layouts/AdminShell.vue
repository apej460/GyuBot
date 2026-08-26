<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isSuperAdmin = computed(() => auth.user?.role === 'SUPER_ADMIN')

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <RouterLink to="/admin/dashboard" class="brand-link">규봇</RouterLink>
        <el-tag size="small" type="warning" round>ADMIN</el-tag>
      </div>

      <el-menu router :default-active="route.path" class="sidebar-menu">
        <el-menu-item index="/admin/dashboard">대시보드</el-menu-item>
        <el-menu-item index="/admin/documents">문서 관리</el-menu-item>
        <el-menu-item index="/admin/members">회원 관리</el-menu-item>
        <el-menu-item index="/admin/managers">관리자 관리</el-menu-item>
        <el-menu-item v-if="isSuperAdmin" index="/admin/system">시스템 관리</el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <p class="footer-label">관리자 센터</p>
        <p class="footer-user">{{ auth.user?.email }} · {{ isSuperAdmin ? '최고관리자' : '관리자' }}</p>
        <el-button size="small" text @click="handleLogout">로그아웃</el-button>
      </div>
    </aside>

    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
}
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #2b3242;
  color: #fff;
  display: flex;
  flex-direction: column;
}
.sidebar-brand {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand-link {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  text-decoration: none;
}
.sidebar-menu {
  flex: 1;
  background: transparent;
  border-right: none;
}
.sidebar-menu :deep(.el-menu-item) {
  color: #c3c9d9;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.sidebar-footer {
  padding: 16px 20px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.footer-label {
  margin: 0;
  font-size: 11px;
  color: #8b93a7;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.footer-user {
  margin: 6px 0 10px;
  font-size: 13px;
  color: #e4e7ed;
  word-break: break-all;
}
.sidebar-footer .el-button {
  color: #c3c9d9;
  padding: 0;
}
.admin-main {
  flex: 1;
  background: #f5f7fa;
  padding: 32px 40px;
  min-width: 0;
}
</style>
