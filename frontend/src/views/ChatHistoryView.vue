<script setup>
import { onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()

onMounted(() => {
  chatStore.fetchSessions()
})

function formatDate(iso) {
  return new Date(iso).toLocaleString('ko-KR')
}
</script>

<template>
  <div class="page">
    <nav>
      <RouterLink to="/chat">← AI 질의</RouterLink>
    </nav>
    <h1>질의 이력</h1>

    <p v-if="chatStore.loadingHistory">불러오는 중…</p>
    <p v-else-if="chatStore.sessions.length === 0" class="empty">아직 대화 이력이 없습니다.</p>

    <ul class="sessions">
      <li v-for="session in chatStore.sessions" :key="session.id">
        <RouterLink :to="{ name: 'chat', params: { id: session.id } }">
          <span class="title">{{ session.title }}</span>
          <span class="date">{{ formatDate(session.createdAt) }}</span>
        </RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 40px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 16px;
}
nav {
  display: flex;
  gap: 12px;
}
.empty {
  color: #999;
}
.sessions {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sessions a {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  text-decoration: none;
  color: inherit;
  border-bottom: 1px solid #eee;
}
.sessions a:hover {
  background: #f8f8f8;
}
.title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.date {
  flex-shrink: 0;
  color: #999;
  font-size: 12px;
}
</style>
