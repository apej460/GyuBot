<script setup>
import { onMounted } from 'vue'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()

onMounted(() => {
  memberStore.fetchList()
})

async function toggleStatus(member) {
  const next = member.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  await memberStore.updateStatus(member.id, next)
}
</script>

<template>
  <div class="page">
    <nav><RouterLink to="/">← 홈</RouterLink></nav>
    <h1>회원 관리</h1>

    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>이름</th>
          <th>이메일</th>
          <th>역할</th>
          <th>상태</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in memberStore.members" :key="m.id">
          <td>{{ m.id }}</td>
          <td>{{ m.name }}</td>
          <td>{{ m.email }}</td>
          <td>{{ m.role }}</td>
          <td>
            <span :class="['status', m.status === 'ACTIVE' ? 'active' : 'suspended']">{{ m.status }}</span>
          </td>
          <td>
            <button @click="toggleStatus(m)">
              {{ m.status === 'ACTIVE' ? '정지' : '활성화' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 60px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th,
td {
  text-align: left;
  padding: 8px;
  border-bottom: 1px solid #eee;
}
button {
  padding: 6px 10px;
  cursor: pointer;
}
.status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status.active {
  background: #e8f5e9;
  color: #2e7d32;
}
.status.suspended {
  background: #ffebee;
  color: #c0392b;
}
</style>
