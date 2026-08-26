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

function landingRoute() {
  const role = auth.user?.role
  return role === 'ADMIN' || role === 'SUPER_ADMIN' ? { name: 'admin-dashboard' } : { name: 'chat' }
}

async function submitLogin() {
  submitting.value = true
  try {
    const data = await auth.login(email.value, password.value)
    if (!data.otpRequired) {
      router.push(landingRoute())
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
    router.push(landingRoute())
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
      <div class="brand">
        <span class="brand-icon">📖</span>
        <span class="brand-name">규봇.</span>
      </div>

      <template v-if="!auth.otpPending">
        <h1 class="title">반갑습니다.</h1>
        <p class="subtitle">사내 규정·문서 기반 AI 지식검색 서비스</p>

        <el-form label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="회사 이메일">
            <el-input v-model="email" type="email" autocomplete="username" />
          </el-form-item>
          <el-form-item label="비밀번호">
            <el-input v-model="password" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
            로그인
          </el-button>
        </el-form>

        <div class="links">
          <RouterLink to="/signup/email">회원가입</RouterLink>
          <RouterLink to="/reset-password">비밀번호 재설정</RouterLink>
        </div>
      </template>

      <template v-else>
        <h1 class="title">인증번호를 입력해주세요</h1>
        <p class="subtitle">{{ auth.pendingEmail }} 로 발송된 인증번호를 입력해주세요.</p>
        <el-form @submit.prevent="submitOtp">
          <el-form-item label="인증번호">
            <el-input v-model="otp" inputmode="numeric" maxlength="6" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
            인증하기
          </el-button>
        </el-form>
      </template>

      <el-alert v-if="auth.error" :title="auth.error" type="error" show-icon :closable="false" class="alert" />

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
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}
.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #303849;
  font-size: 16px;
}
.brand-name {
  font-size: 18px;
  font-weight: 700;
}
.title {
  margin: 0;
  font-size: 22px;
}
.subtitle {
  margin: 4px 0 24px;
  color: #909399;
  font-size: 13px;
}
.submit-btn {
  width: 100%;
}
.links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
}
.alert {
  margin-top: 12px;
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
