<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="home">
    <h1>GyuBot</h1>
    <p>로그인 성공</p>
    <ul>
      <li>userId: {{ auth.user?.userId }}</li>
      <li>email: {{ auth.user?.email }}</li>
      <li>companyId: {{ auth.user?.companyId }}</li>
      <li>role: {{ auth.user?.role }}</li>
    </ul>
    <button @click="handleLogout">로그아웃</button>
  </div>
</template>

<style scoped>
.home {
  max-width: 360px;
  margin: 80px auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
