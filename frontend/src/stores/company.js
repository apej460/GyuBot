import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useCompanyStore = defineStore('company', () => {
  const companies = ref([])
  const error = ref('')

  async function fetchList() {
    const { data } = await http.get('/api/companies')
    companies.value = data
    return data
  }

  async function register({ name, emailDomain }) {
    error.value = ''
    try {
      await http.post('/api/companies', { name, emailDomain })
      await fetchList()
    } catch (e) {
      error.value = e.response?.data?.message ?? '회사 등록에 실패했습니다.'
      throw e
    }
  }

  return { companies, error, fetchList, register }
})
