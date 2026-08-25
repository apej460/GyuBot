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
    <nav><RouterLink to="/">← 홈</RouterLink></nav>
    <h1>가입 승인</h1>

    <p v-if="signupStore.requests.length === 0" class="empty">대기 중인 가입 신청이 없습니다.</p>

    <ul class="requests">
      <li v-for="r in signupStore.requests" :key="r.id">
        <div class="info">
          <strong>{{ r.name }}</strong>
          <span>{{ r.email }}</span>
          <a :href="`/api/users/signup-requests/${r.id}/attachment`" target="_blank"
            >첨부파일 보기 ({{ r.attachmentFilename }})</a
          >
        </div>

        <div class="actions" v-if="rejectingId !== r.id">
          <button @click="handleApprove(r.id)">승인</button>
          <button @click="startReject(r.id)">반려</button>
        </div>
        <div class="reject-form" v-else>
          <input v-model="reason" placeholder="반려 사유" />
          <button @click="confirmReject">반려 확정</button>
          <button @click="rejectingId = null">취소</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page {
  max-width: 560px;
  margin: 60px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.empty {
  color: #666;
  font-size: 14px;
}
.requests {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.requests li {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 14px;
}
.info span {
  color: #666;
}
.actions {
  display: flex;
  gap: 8px;
}
.reject-form {
  display: flex;
  gap: 8px;
}
.reject-form input {
  padding: 6px;
  font-size: 13px;
}
button {
  padding: 6px 10px;
  cursor: pointer;
}
</style>
