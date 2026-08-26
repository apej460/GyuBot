import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useEmailSignupStore = defineStore('emailSignup', () => {
  const error = ref('')
  const companyName = ref('')

  async function resolveCompany(email) {
    error.value = ''
    try {
      const { data } = await http.get('/api/users/signup/email/company', { params: { email } })
      companyName.value = data.name
      return data.name
    } catch (e) {
      error.value = e.response?.data?.message ?? '등록되지 않은 회사 이메일입니다.'
      throw e
    }
  }

  async function requestOtp(email) {
    error.value = ''
    try {
      await http.post('/api/users/signup/email/otp', { email })
    } catch (e) {
      error.value = e.response?.data?.message ?? '인증번호 발송에 실패했습니다.'
      throw e
    }
  }

  async function complete({ email, code, password, name, department, position }) {
    error.value = ''
    try {
      await http.post('/api/users/signup/email/complete', {
        email,
        code,
        password,
        name,
        department,
        position,
      })
    } catch (e) {
      error.value = e.response?.data?.message ?? '회원가입에 실패했습니다.'
      throw e
    }
  }

  return { error, companyName, resolveCompany, requestOtp, complete }
})
