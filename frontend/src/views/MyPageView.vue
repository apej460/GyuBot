<script setup>
import { onMounted, ref } from 'vue'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()

const currentPassword = ref('')
const newPassword = ref('')
const submitting = ref(false)
const success = ref('')

onMounted(() => {
  memberStore.fetchMe()
})

async function submitChangePassword() {
  submitting.value = true
  success.value = ''
  try {
    await memberStore.changePassword(currentPassword.value, newPassword.value)
    success.value = '비밀번호가 변경되었습니다.'
    currentPassword.value = ''
    newPassword.value = ''
  } catch {
    // memberStore.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <nav><RouterLink to="/">← 홈</RouterLink></nav>
    <h1>내 정보</h1>

    <dl v-if="memberStore.me">
      <dt>이름</dt>
      <dd>{{ memberStore.me.name }}</dd>
      <dt>이메일</dt>
      <dd>{{ memberStore.me.email }}</dd>
      <dt>회사 ID</dt>
      <dd>{{ memberStore.me.companyId }}</dd>
      <dt>역할</dt>
      <dd>{{ memberStore.me.role }}</dd>
      <dt>상태</dt>
      <dd>{{ memberStore.me.status }}</dd>
    </dl>

    <h2>비밀번호 변경</h2>
    <form @submit.prevent="submitChangePassword">
      <label>
        현재 비밀번호
        <input v-model="currentPassword" type="password" required autocomplete="current-password" />
      </label>
      <label>
        새 비밀번호 (8자 이상)
        <input v-model="newPassword" type="password" required minlength="8" autocomplete="new-password" />
      </label>
      <button type="submit" :disabled="submitting">변경하기</button>
    </form>

    <p v-if="success" class="success">{{ success }}</p>
    <p v-if="memberStore.error" class="error">{{ memberStore.error }}</p>
  </div>
</template>

<style scoped>
.page {
  max-width: 420px;
  margin: 60px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
  font-size: 14px;
  margin: 0;
}
dt {
  color: #666;
}
dd {
  margin: 0;
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
.success {
  color: #2e7d32;
}
.error {
  color: #c0392b;
}
</style>
