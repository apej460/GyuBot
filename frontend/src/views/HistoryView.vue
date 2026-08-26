<script setup>
import { computed, onMounted, ref } from 'vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const keyword = ref('')
const dateRange = ref([])

onMounted(() => {
  chatStore.fetchSessions()
})

function formatDate(iso) {
  return new Date(iso).toLocaleString('ko-KR')
}

const filteredSessions = computed(() => {
  return chatStore.sessions.filter((session) => {
    if (keyword.value && !session.title.toLowerCase().includes(keyword.value.toLowerCase())) {
      return false
    }
    if (dateRange.value?.length === 2) {
      const createdAt = new Date(session.createdAt).getTime()
      const [start, end] = dateRange.value
      if (createdAt < start.getTime() || createdAt > end.getTime() + 86400000) {
        return false
      }
    }
    return true
  })
})
</script>

<template>
  <div class="page">
    <h1>질의 이력</h1>

    <div class="filters">
      <el-input v-model="keyword" placeholder="질문 검색" clearable class="keyword-input" />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="~"
        start-placeholder="시작일"
        end-placeholder="종료일"
      />
    </div>

    <el-skeleton v-if="chatStore.loadingHistory" :rows="4" animated />
    <el-empty v-else-if="filteredSessions.length === 0" description="조건에 맞는 대화 이력이 없습니다." />

    <el-card v-else shadow="never">
      <el-table :data="filteredSessions" style="width: 100%" @row-click="(row) => $router.push({ name: 'chat', params: { id: row.id } })">
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
.filters {
  display: flex;
  gap: 12px;
}
.keyword-input {
  max-width: 280px;
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
