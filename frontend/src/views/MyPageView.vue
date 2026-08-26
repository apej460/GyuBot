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
    <h1>내 정보</h1>

    <el-card v-if="memberStore.me" shadow="never" class="section">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="이름">{{ memberStore.me.name }}</el-descriptions-item>
        <el-descriptions-item label="이메일">{{ memberStore.me.email }}</el-descriptions-item>
        <el-descriptions-item label="회사 ID">{{ memberStore.me.companyId }}</el-descriptions-item>
        <el-descriptions-item label="역할">{{ memberStore.me.role }}</el-descriptions-item>
        <el-descriptions-item label="상태">
          <el-tag :type="memberStore.me.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ memberStore.me.status }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="section">
      <h2 class="section-title">비밀번호 변경</h2>
      <el-form label-position="top" @submit.prevent="submitChangePassword">
        <el-form-item label="현재 비밀번호">
          <el-input v-model="currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="새 비밀번호 (8자 이상)">
          <el-input v-model="newPassword" type="password" show-password minlength="8" autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting">변경하기</el-button>
      </el-form>

      <el-alert v-if="success" :title="success" type="success" show-icon :closable="false" class="alert" />
      <el-alert v-if="memberStore.error" :title="memberStore.error" type="error" show-icon :closable="false" class="alert" />
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 480px;
}
.page h1 {
  margin: 0;
  font-size: 22px;
}
.section-title {
  margin: 0 0 16px;
  font-size: 16px;
}
.alert {
  margin-top: 12px;
}
</style>
