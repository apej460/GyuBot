import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const asking = ref(false)
  const loadingHistory = ref(false)
  const error = ref('')

  async function fetchSessions() {
    loadingHistory.value = true
    try {
      const { data } = await http.get('/api/chat/sessions')
      sessions.value = data
      return data
    } finally {
      loadingHistory.value = false
    }
  }

  async function loadSession(sessionId) {
    loadingHistory.value = true
    try {
      const { data } = await http.get(`/api/chat/sessions/${sessionId}/messages`)
      currentSessionId.value = sessionId
      messages.value = data
      return data
    } finally {
      loadingHistory.value = false
    }
  }

  function startNewSession() {
    currentSessionId.value = null
    messages.value = []
    error.value = ''
  }

  async function ask(question) {
    error.value = ''
    const userMessage = { id: `pending-${Date.now()}`, role: 'USER', content: question, sources: [] }
    messages.value.push(userMessage)
    asking.value = true
    try {
      const { data } = await http.post('/api/chat/messages', {
        sessionId: currentSessionId.value,
        question,
      })
      currentSessionId.value = data.sessionId
      messages.value.push({
        id: data.messageId,
        role: 'ASSISTANT',
        content: data.answer,
        sources: data.sources,
      })
      return data
    } catch (e) {
      messages.value = messages.value.filter((m) => m !== userMessage)
      error.value = e.response?.data?.message ?? '답변을 가져오지 못했습니다.'
      throw e
    } finally {
      asking.value = false
    }
  }

  async function fetchCitation(documentId) {
    const { data } = await http.get(`/api/documents/${documentId}/citation`)
    return data
  }

  return {
    sessions,
    currentSessionId,
    messages,
    asking,
    loadingHistory,
    error,
    fetchSessions,
    loadSession,
    startNewSession,
    ask,
    fetchCitation,
  }
})
