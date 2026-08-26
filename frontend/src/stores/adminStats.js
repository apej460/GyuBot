import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useAdminStatsStore = defineStore('adminStats', () => {
  const memberCount = ref(0)
  const documentCount = ref(0)
  const todayQuestionCount = ref(0)
  const unansweredCount = ref(0)
  const dailyLast7 = ref([])
  const topKeywords = ref([])
  const loading = ref(false)

  async function fetchAll() {
    loading.value = true
    try {
      const [members, documents, chat] = await Promise.all([
        http.get('/api/users/stats'),
        http.get('/api/documents/stats'),
        http.get('/api/chat/stats'),
      ])
      memberCount.value = members.data.totalCount
      documentCount.value = documents.data.totalCount
      todayQuestionCount.value = chat.data.todayCount
      unansweredCount.value = chat.data.unansweredCount
      dailyLast7.value = chat.data.dailyLast7
      topKeywords.value = chat.data.topKeywords
    } finally {
      loading.value = false
    }
  }

  return {
    memberCount,
    documentCount,
    todayQuestionCount,
    unansweredCount,
    dailyLast7,
    topKeywords,
    loading,
    fetchAll,
  }
})
