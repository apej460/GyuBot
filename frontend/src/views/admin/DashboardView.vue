<script setup>
import { computed, onMounted } from 'vue'
import { useAdminStatsStore } from '@/stores/adminStats'

const stats = useAdminStatsStore()

onMounted(() => {
  stats.fetchAll()
})

const maxDailyCount = computed(() => Math.max(1, ...stats.dailyLast7.map((d) => d.count)))

function formatDay(dateStr) {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<template>
  <div class="page">
    <h1>대시보드</h1>

    <el-skeleton v-if="stats.loading" :rows="6" animated />

    <template v-else>
      <div class="stat-grid">
        <el-card shadow="never" class="stat-card">
          <p class="stat-label">전체 회원</p>
          <p class="stat-value">{{ stats.memberCount }}</p>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <p class="stat-label">등록 문서</p>
          <p class="stat-value">{{ stats.documentCount }}</p>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <p class="stat-label">금일 질의</p>
          <p class="stat-value">{{ stats.todayQuestionCount }}</p>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <p class="stat-label">미답변 질문</p>
          <p class="stat-value warn">{{ stats.unansweredCount }}</p>
        </el-card>
      </div>

      <div class="panel-grid">
        <el-card shadow="never">
          <h2 class="panel-title">최근 7일 질의량</h2>
          <div class="bar-chart">
            <div v-for="d in stats.dailyLast7" :key="d.date" class="bar-column">
              <div class="bar" :style="{ height: (d.count / maxDailyCount) * 100 + '%' }" />
              <span class="bar-count">{{ d.count }}</span>
              <span class="bar-label">{{ formatDay(d.date) }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <h2 class="panel-title">자주 검색된 키워드</h2>
          <el-empty v-if="stats.topKeywords.length === 0" description="아직 데이터가 없습니다." />
          <ol v-else class="keyword-list">
            <li v-for="k in stats.topKeywords" :key="k.keyword">
              <span class="keyword">{{ k.keyword }}</span>
              <span class="keyword-count">{{ k.count }}</span>
            </li>
          </ol>
        </el-card>
      </div>
    </template>
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
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #909399;
}
.stat-value {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}
.stat-value.warn {
  color: var(--el-color-danger);
}
.panel-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
.panel-title {
  margin: 0 0 20px;
  font-size: 15px;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  height: 180px;
}
.bar-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}
.bar {
  width: 100%;
  max-width: 36px;
  background: var(--el-color-primary);
  border-radius: 4px 4px 0 0;
  min-height: 2px;
}
.bar-count {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 600;
}
.bar-label {
  margin-top: 2px;
  font-size: 11px;
  color: #909399;
}
.keyword-list {
  margin: 0;
  padding: 0 0 0 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.keyword-list li {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}
.keyword-count {
  color: #909399;
}
</style>
