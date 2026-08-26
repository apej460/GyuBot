import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useSignupStore = defineStore('signup', () => {
  const requests = ref([])
  const error = ref('')
  const successMessage = ref('')

  async function submit({ email, name, companyName, department, position, password, attachment }) {
    error.value = ''
    successMessage.value = ''
    try {
      const formData = new FormData()
      formData.append('email', email)
      formData.append('name', name)
      if (companyName) formData.append('companyName', companyName)
      if (department) formData.append('department', department)
      if (position) formData.append('position', position)
      formData.append('password', password)
      formData.append('attachment', attachment)
      await http.post('/api/users/signup-requests', formData)
      successMessage.value = '가입 신청이 접수되었습니다. 관리자 승인 후 로그인할 수 있습니다.'
    } catch (e) {
      error.value = e.response?.data?.message ?? '가입 신청에 실패했습니다.'
      throw e
    }
  }

  async function fetchPending() {
    const { data } = await http.get('/api/users/signup-requests')
    requests.value = data
    return data
  }

  async function approve(id) {
    await http.post(`/api/users/signup-requests/${id}/approve`)
    await fetchPending()
  }

  async function reject(id, reason) {
    await http.post(`/api/users/signup-requests/${id}/reject`, { reason })
    await fetchPending()
  }

  return { requests, error, successMessage, submit, fetchPending, approve, reject }
})
