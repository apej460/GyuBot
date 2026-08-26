<script setup>
import { onMounted, ref } from 'vue'
import { useSignupStore } from '@/stores/signup'

const signupStore = useSignupStore()
const rejectingId = ref(null)
const reason = ref('')

onMounted(() => {
  signupStore.fetchPending()
})

async function handleApprove(id) {
  await signupStore.approve(id)
}

function startReject(id) {
  rejectingId.value = id
  reason.value = ''
}

async function confirmReject() {
  await signupStore.reject(rejectingId.value, reason.value)
  rejectingId.value = null
}
</script>

<template>
  <div class="page">
    <h1>가입 승인</h1>

    <el-empty v-if="signupStore.requests.length === 0" description="대기 중인 가입 신청이 없습니다." />

    <el-card v-for="r in signupStore.requests" :key="r.id" shadow="never" class="request-card">
      <div class="request">
        <div class="info">
          <strong>{{ r.name }}</strong>
          <span class="email">{{ r.email }}</span>
          <a :href="`/api/users/signup-requests/${r.id}/attachment`" target="_blank" class="attachment-link">
            첨부파일 보기 ({{ r.attachmentFilename }})
          </a>
        </div>

        <div v-if="rejectingId !== r.id" class="actions">
          <el-button type="primary" size="small" @click="handleApprove(r.id)">승인</el-button>
          <el-button size="small" @click="startReject(r.id)">반려</el-button>
        </div>
        <div v-else class="reject-form">
          <el-input v-model="reason" placeholder="반려 사유" size="small" />
          <el-button type="danger" size="small" @click="confirmReject">반려 확정</el-button>
          <el-button size="small" @click="rejectingId = null">취소</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page h1 {
  margin: 0;
  font-size: 22px;
}
.request-card :deep(.el-card__body) {
  padding: 16px;
}
.request {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}
.email {
  color: #909399;
}
.attachment-link {
  font-size: 13px;
}
.actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.reject-form {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.reject-form .el-input {
  width: 180px;
}
</style>
