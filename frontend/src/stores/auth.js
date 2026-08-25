import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const otpPending = ref(false)
  const pendingEmail = ref('')
  const initialized = ref(false)
  const error = ref('')

  async function login(email, password) {
    error.value = ''
    try {
      const { data } = await http.post('/api/auth/login', { email, password })
      if (data.otpRequired) {
        otpPending.value = true
        pendingEmail.value = email
      } else {
        otpPending.value = false
        await checkAuth()
      }
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? '로그인에 실패했습니다.'
      throw e
    }
  }

  async function verifyOtp(code) {
    error.value = ''
    try {
      const { data } = await http.post('/api/auth/otp/verify', {
        email: pendingEmail.value,
        code,
      })
      otpPending.value = false
      await checkAuth()
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? '인증에 실패했습니다.'
      throw e
    }
  }

  async function checkAuth() {
    try {
      const { data } = await http.get('/api/auth/check')
      user.value = data
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function logout() {
    await http.post('/api/auth/logout')
    user.value = null
    otpPending.value = false
    pendingEmail.value = ''
  }

  return { user, otpPending, pendingEmail, initialized, error, login, verifyOtp, checkAuth, logout }
})
