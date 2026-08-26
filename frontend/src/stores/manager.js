import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useManagerStore = defineStore('manager', () => {
  const managers = ref([])
  const error = ref('')

  async function fetchList() {
    const { data } = await http.get('/api/users/managers')
    managers.value = data
    return data
  }

  async function create({ email, name, password, role }) {
    error.value = ''
    try {
      await http.post('/api/users/managers', { email, name, password, role })
      await fetchList()
    } catch (e) {
      error.value = e.response?.data?.message ?? '관리자 계정 생성에 실패했습니다.'
      throw e
    }
  }

  return { managers, error, fetchList, create }
})
