import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const usePasswordResetStore = defineStore('passwordReset', () => {
  const error = ref('')

  async function requestOtp(email) {
    error.value = ''
    try {
      await http.post('/api/auth/password-reset/request', { email })
    } catch (e) {
      error.value = e.response?.data?.message ?? '인증번호 발송에 실패했습니다.'
      throw e
    }
  }

  // 이메일+인증번호+새 비밀번호를 한 번에 검증·적용한다 (백엔드에 별도의 "인증만" 하는
  // 엔드포인트가 없어서, 인증번호가 틀렸는지는 이 마지막 제출 시점에야 알 수 있다).
  async function confirm(email, code, newPassword) {
    error.value = ''
    try {
      await http.post('/api/auth/password-reset/confirm', { email, code, newPassword })
    } catch (e) {
      error.value = e.response?.data?.message ?? '비밀번호 재설정에 실패했습니다.'
      throw e
    }
  }

  return { error, requestOtp, confirm }
})
