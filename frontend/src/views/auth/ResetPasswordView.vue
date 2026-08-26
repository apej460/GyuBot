<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePasswordResetStore } from '@/stores/passwordReset'

const store = usePasswordResetStore()
const router = useRouter()

const step = ref(1) // 1: 이메일 인증  2: 새 비밀번호  3: 완료
const email = ref('')
const code = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const submitting = ref(false)
const otpSent = ref(false)

const passwordMismatch = computed(
  () => newPasswordConfirm.value.length > 0 && newPassword.value !== newPasswordConfirm.value,
)

async function sendOtp() {
  submitting.value = true
  try {
    await store.requestOtp(email.value)
    otpSent.value = true
  } catch {
    // store.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}

function goToPasswordStep() {
  store.error = ''
  step.value = 2
}

async function submitReset() {
  if (passwordMismatch.value) return
  submitting.value = true
  try {
    await store.confirm(email.value, code.value, newPassword.value)
    step.value = 3
  } catch {
    // 인증번호가 틀렸을 경우도 여기서 store.error로 표시됨
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

      <template v-if="step === 1">
        <h1 class="title">비밀번호 재설정</h1>
        <p class="subtitle">가입하신 이메일로 인증번호를 보내드립니다.</p>

        <el-form label-position="top" @submit.prevent="otpSent ? goToPasswordStep() : sendOtp()">
          <el-form-item label="이메일">
            <el-input v-model="email" type="email" autocomplete="username" :disabled="otpSent" />
          </el-form-item>

          <template v-if="!otpSent">
            <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
              인증번호 발송
            </el-button>
          </template>
          <template v-else>
            <el-form-item label="인증번호">
              <el-input v-model="code" inputmode="numeric" maxlength="6" />
            </el-form-item>
            <el-button type="primary" native-type="submit" class="submit-btn">다음</el-button>
            <el-button text @click="sendOtp" :loading="submitting" class="resend-btn">
              인증번호 재발송
            </el-button>
          </template>
        </el-form>
      </template>

      <template v-else-if="step === 2">
        <h1 class="title">새 비밀번호 설정</h1>
        <p class="subtitle">{{ email }} 계정의 새 비밀번호를 입력해주세요.</p>

        <el-form label-position="top" @submit.prevent="submitReset">
          <el-form-item label="새 비밀번호">
            <el-input v-model="newPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="새 비밀번호 확인" :error="passwordMismatch ? '비밀번호가 일치하지 않습니다.' : ''">
            <el-input v-model="newPasswordConfirm" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
            비밀번호 재설정
          </el-button>
        </el-form>
      </template>

      <template v-else>
        <h1 class="title">비밀번호가 변경되었습니다</h1>
        <p class="subtitle">새 비밀번호로 다시 로그인해주세요.</p>
        <el-button type="primary" class="submit-btn" @click="router.push({ name: 'login' })">
          로그인하러 가기
        </el-button>
      </template>

      <el-alert v-if="store.error" :title="store.error" type="error" show-icon :closable="false" class="alert" />

      <p class="links" v-if="step !== 3">
        <RouterLink to="/login">로그인으로 돌아가기</RouterLink>
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
.resend-btn {
  width: 100%;
  margin-top: 4px;
  margin-left: 0;
}
.alert {
  margin-top: 12px;
}
.links {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}
</style>
