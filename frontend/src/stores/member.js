import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useMemberStore = defineStore('member', () => {
  const me = ref(null)
  const members = ref([])
  const error = ref('')

  async function fetchMe() {
    const { data } = await http.get('/api/users/me')
    me.value = data
    return data
  }

  async function changePassword(currentPassword, newPassword) {
    error.value = ''
    try {
      await http.patch('/api/users/me/password', { currentPassword, newPassword })
    } catch (e) {
      error.value = e.response?.data?.message ?? '비밀번호 변경에 실패했습니다.'
      throw e
    }
  }

  async function fetchList() {
    const { data } = await http.get('/api/users')
    members.value = data
    return data
  }

  async function updateStatus(id, status) {
    await http.patch(`/api/users/${id}/status`, { status })
    await fetchList()
  }

  return { me, members, error, fetchMe, changePassword, fetchList, updateStatus }
})
