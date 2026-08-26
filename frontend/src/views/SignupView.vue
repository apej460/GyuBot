<script setup>
import { ref } from 'vue'
import { useSignupStore } from '@/stores/signup'

const signupStore = useSignupStore()

const email = ref('')
const name = ref('')
const password = ref('')
const attachment = ref(null)
const submitting = ref(false)
const submitted = ref(false)

function handleFileChange(file) {
  attachment.value = file.raw
}

async function submitSignup() {
  submitting.value = true
  try {
    await signupStore.submit({
      email: email.value,
      name: name.value,
      password: password.value,
      attachment: attachment.value,
    })
    submitted.value = true
  } catch {
    // signupStore.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1 class="title">가입 신청</h1>
      <p class="hint-text">
        회사 이메일이 없는 경우, 명함이나 재직증명서를 첨부해 예외 가입을 신청할 수 있습니다.
        관리자 승인 후 로그인할 수 있습니다.
      </p>

      <el-form v-if="!submitted" label-position="top" @submit.prevent="submitSignup">
        <el-form-item label="이메일">
          <el-input v-model="email" type="email" autocomplete="email" />
        </el-form-item>
        <el-form-item label="이름">
          <el-input v-model="name" autocomplete="name" />
        </el-form-item>
        <el-form-item label="비밀번호 (8자 이상)">
          <el-input v-model="password" type="password" show-password minlength="8" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="명함 / 재직증명서 (JPG, PNG, PDF)">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".jpg,.jpeg,.png,.pdf"
            :on-change="handleFileChange"
            class="upload"
          >
            <p class="upload-text">파일을 드래그하거나 클릭해서 첨부하세요</p>
          </el-upload>
        </el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="!attachment"
          class="submit-btn"
        >
          가입 신청
        </el-button>
      </el-form>

      <el-result v-else icon="success" :title="signupStore.successMessage" />

      <el-alert
        v-if="signupStore.error"
        :title="signupStore.error"
        type="error"
        show-icon
        :closable="false"
        class="alert"
      />

      <RouterLink to="/login" class="back-link">← 로그인으로 돌아가기</RouterLink>
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
.upload {
  width: 100%;
}
.upload-text {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.submit-btn {
  width: 100%;
}
.alert {
  margin-top: 12px;
}
.back-link {
  display: block;
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}
</style>
