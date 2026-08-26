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
    <h1>질의 이력</h1>

    <el-skeleton v-if="chatStore.loadingHistory" :rows="4" animated />
    <el-empty v-else-if="chatStore.sessions.length === 0" description="아직 대화 이력이 없습니다." />

    <el-card v-else shadow="never">
      <el-table :data="chatStore.sessions" style="width: 100%" @row-click="(row) => $router.push({ name: 'chat', params: { id: row.id } })">
        <el-table-column label="질문">
          <template #default="{ row }">
            <RouterLink :to="{ name: 'chat', params: { id: row.id } }" class="title-link">
              {{ row.title }}
            </RouterLink>
          </template>
        </el-table-column>
        <el-table-column label="일시" width="200">
          <template #default="{ row }">
            <span class="date">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page h1 {
  margin: 0;
  font-size: 22px;
}
.title-link {
  color: inherit;
  text-decoration: none;
}
.title-link:hover {
  color: var(--el-color-primary);
}
.date {
  color: #909399;
  font-size: 13px;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
