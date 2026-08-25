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

function handleFileChange(e) {
  attachment.value = e.target.files[0] ?? null
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
  <div class="signup">
    <h1>가입 신청</h1>
    <p class="hint">
      회사 이메일이 없는 경우, 명함이나 재직증명서를 첨부해 예외 가입을 신청할 수 있습니다.
      관리자 승인 후 로그인할 수 있습니다.
    </p>

    <form v-if="!submitted" @submit.prevent="submitSignup">
      <label>
        이메일
        <input v-model="email" type="email" required autocomplete="email" />
      </label>
      <label>
        이름
        <input v-model="name" type="text" required autocomplete="name" />
      </label>
      <label>
        비밀번호 (8자 이상)
        <input v-model="password" type="password" required minlength="8" autocomplete="new-password" />
      </label>
      <label>
        명함 / 재직증명서 (JPG, PNG, PDF)
        <input type="file" accept=".jpg,.jpeg,.png,.pdf" required @change="handleFileChange" />
      </label>
      <button type="submit" :disabled="submitting">가입 신청</button>
    </form>

    <p v-else class="success">{{ signupStore.successMessage }}</p>
    <p v-if="signupStore.error" class="error">{{ signupStore.error }}</p>

    <RouterLink to="/login">← 로그인으로 돌아가기</RouterLink>
  </div>
</template>

<style scoped>
.signup {
  max-width: 380px;
  margin: 60px auto;
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
.hint {
  font-size: 13px;
  color: #666;
}
.success {
  color: #2e7d32;
}
.error {
  color: #c0392b;
}
</style>
