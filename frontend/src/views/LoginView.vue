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
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1 class="title">GyuBot</h1>
      <p class="subtitle">사내 규정 AI 질의응답</p>

      <el-form v-if="!auth.otpPending" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="이메일">
          <el-input v-model="email" type="email" autocomplete="username" placeholder="you@company.com" />
        </el-form-item>
        <el-form-item label="비밀번호">
          <el-input v-model="password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
          로그인
        </el-button>
      </el-form>

      <el-form v-else @submit.prevent="submitOtp">
        <p class="otp-hint">{{ auth.pendingEmail }} 로 발송된 인증번호를 입력해주세요.</p>
        <el-form-item label="인증번호">
          <el-input v-model="otp" inputmode="numeric" maxlength="6" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
          인증하기
        </el-button>
      </el-form>

      <el-alert v-if="auth.error" :title="auth.error" type="error" show-icon :closable="false" class="alert" />

      <RouterLink v-if="!auth.otpPending" to="/signup" class="signup-link">
        회사 이메일이 없으신가요? 가입 신청하기
      </RouterLink>

      <p class="hint">
        테스트 계정 — 임직원: employee@gyubot.local / Passw0rd!<br />
        관리자: admin@gyubot.local / Passw0rd! (OTP는
        <a href="http://localhost:8025" target="_blank">Mailpit</a>에서 확인)
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.auth-card {
  width: 380px;
}
.title {
  margin: 0;
  font-size: 22px;
  text-align: center;
}
.subtitle {
  margin: 4px 0 24px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.submit-btn {
  width: 100%;
}
.otp-hint {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px;
}
.alert {
  margin-top: 12px;
}
.signup-link {
  display: block;
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}
.hint {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
