<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const otp = ref('')
const submitting = ref(false)

async function submitLogin() {
  submitting.value = true
  try {
    await auth.login(email.value, password.value)
    if (!auth.otpPending) {
      router.push({ name: 'home' })
    }
  } catch {
    // auth.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}

async function submitOtp() {
  submitting.value = true
  try {
    await auth.verifyOtp(otp.value)
    router.push({ name: 'home' })
  } catch {
    // auth.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <h1>GyuBot 로그인</h1>

    <form v-if="!auth.otpPending" @submit.prevent="submitLogin">
      <label>
        이메일
        <input v-model="email" type="email" required autocomplete="username" />
      </label>
      <label>
        비밀번호
        <input v-model="password" type="password" required autocomplete="current-password" />
      </label>
      <button type="submit" :disabled="submitting">로그인</button>
    </form>

    <form v-else @submit.prevent="submitOtp">
      <p>{{ auth.pendingEmail }} 로 발송된 인증번호를 입력해주세요.</p>
      <label>
        인증번호
        <input v-model="otp" inputmode="numeric" maxlength="6" required />
      </label>
      <button type="submit" :disabled="submitting">인증하기</button>
    </form>

    <p v-if="auth.error" class="error">{{ auth.error }}</p>

    <RouterLink v-if="!auth.otpPending" to="/signup">회사 이메일이 없으신가요? 가입 신청하기</RouterLink>

    <p class="hint">
      테스트 계정 — 임직원: employee@gyubot.local / Passw0rd!<br />
      관리자: admin@gyubot.local / Passw0rd! (OTP는
      <a href="http://localhost:8025" target="_blank">Mailpit</a>에서 확인)
    </p>
  </div>
</template>

<style scoped>
.login {
  max-width: 360px;
  margin: 80px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}
input {
  padding: 8px;
  font-size: 14px;
}
button {
  padding: 10px;
  cursor: pointer;
}
.error {
  color: #c0392b;
}
.hint {
  font-size: 12px;
  color: #666;
}
</style>
