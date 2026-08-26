<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <header class="shell-header">
      <div class="shell-header-inner">
        <RouterLink to="/chat" class="brand">규봇</RouterLink>
        <el-menu mode="horizontal" :ellipsis="false" router :default-active="route.path" class="shell-menu">
          <el-menu-item index="/chat">AI 질의</el-menu-item>
          <el-menu-item index="/history">질의 이력</el-menu-item>
          <el-menu-item index="/mypage">내 정보</el-menu-item>
        </el-menu>
        <div class="shell-user">
          <span class="shell-user-email">{{ auth.user?.email }}</span>
          <el-button size="small" text @click="handleLogout">로그아웃</el-button>
        </div>
      </div>
    </header>
    <main class="shell-main">
      <RouterView />
    </main>
  </div>
</template>

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
