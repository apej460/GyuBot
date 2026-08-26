<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useEmailSignupStore } from '@/stores/emailSignup'

const store = useEmailSignupStore()
const router = useRouter()

const step = ref(1) // 1: 이메일 인증  2: 추가 정보  3: 완료
const email = ref('')
const code = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const department = ref('')
const position = ref('')
const otpSent = ref(false)
const submitting = ref(false)

const passwordMismatch = computed(
  () => passwordConfirm.value.length > 0 && password.value !== passwordConfirm.value,
)

async function sendOtp() {
  submitting.value = true
  try {
    await store.resolveCompany(email.value)
    await store.requestOtp(email.value)
    otpSent.value = true
  } catch {
    // store.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}

function goToProfileStep() {
  if (passwordMismatch.value) return
  store.error = ''
  step.value = 2
}

async function submitComplete() {
  submitting.value = true
  try {
    await store.complete({
      email: email.value,
      code: code.value,
      password: password.value,
      name: name.value,
      department: department.value,
      position: position.value,
    })
    step.value = 3
  } catch {
    // 인증번호가 틀렸을 경우도 여기서 store.error로 표시됨 (다시 1단계로 돌아가지 않고 알림만 표시)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1 class="title">회원가입</h1>
      <p class="hint-text">회사 이메일 도메인이 등록되어 있다면 승인 없이 바로 가입할 수 있습니다.</p>

      <template v-if="step === 1">
        <el-form label-position="top" @submit.prevent="otpSent ? goToProfileStep() : sendOtp()">
          <el-form-item label="회사 이메일">
            <el-input v-model="email" type="email" autocomplete="username" :disabled="otpSent" />
          </el-form-item>
          <el-form-item v-if="otpSent && store.companyName" label="소속 회사">
            <el-input :model-value="store.companyName" disabled />
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
            <el-form-item label="비밀번호 (8자 이상)">
              <el-input v-model="password" type="password" show-password minlength="8" autocomplete="new-password" />
            </el-form-item>
            <el-form-item
              label="비밀번호 확인"
              :error="passwordMismatch ? '비밀번호가 일치하지 않습니다.' : ''"
            >
              <el-input v-model="passwordConfirm" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-button type="primary" native-type="submit" class="submit-btn">다음</el-button>
            <el-button text @click="sendOtp" :loading="submitting" class="resend-btn">
              인증번호 재발송
            </el-button>
          </template>
        </el-form>
      </template>

      <template v-else-if="step === 2">
        <el-form label-position="top" @submit.prevent="submitComplete">
          <el-form-item label="이름">
            <el-input v-model="name" autocomplete="name" />
          </el-form-item>
          <el-form-item label="소속 회사">
            <el-input :model-value="store.companyName" disabled />
          </el-form-item>
          <el-form-item label="부서">
            <el-input v-model="department" />
          </el-form-item>
          <el-form-item label="직급">
            <el-input v-model="position" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" :disabled="!name" class="submit-btn">
            가입 완료
          </el-button>
        </el-form>
      </template>

      <el-result v-else icon="success" title="가입이 완료되었습니다" sub-title="바로 로그인할 수 있습니다.">
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'login' })">로그인하러 가기</el-button>
        </template>
      </el-result>

      <el-alert v-if="store.error" :title="store.error" type="error" show-icon :closable="false" class="alert" />

      <p class="links" v-if="step !== 3">
        <RouterLink to="/login">로그인으로 돌아가기</RouterLink>
        <RouterLink to="/signup/document">회사 이메일이 없으신가요?</RouterLink>
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
  padding: 40px 16px;
}
.auth-card {
  width: 420px;
}
.title {
  margin: 0 0 8px;
  font-size: 20px;
}
.hint-text {
  margin: 0 0 20px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
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
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
</style>
