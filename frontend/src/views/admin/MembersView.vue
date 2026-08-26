<script setup>
import { onMounted, ref } from 'vue'
import { useMemberStore } from '@/stores/member'
import { useSignupStore } from '@/stores/signup'

const memberStore = useMemberStore()
const signupStore = useSignupStore()
const activeTab = ref('members')
const rejectingId = ref(null)
const reason = ref('')

onMounted(() => {
  memberStore.fetchList()
  signupStore.fetchPending()
})

async function toggleStatus(member) {
  const next = member.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  await memberStore.updateStatus(member.id, next)
}

async function handleApprove(id) {
  await signupStore.approve(id)
  await memberStore.fetchList()
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
    <div class="page-header">
      <h1>회원 관리</h1>
      <el-badge v-if="signupStore.requests.length" :value="signupStore.requests.length" class="badge" />
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="활성 회원" name="members">
        <el-card shadow="never">
          <el-table :data="memberStore.members" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="이름" width="120" />
            <el-table-column prop="email" label="이메일" />
            <el-table-column prop="department" label="부서" width="110">
              <template #default="{ row }">{{ row.department || '-' }}</template>
            </el-table-column>
            <el-table-column prop="position" label="직급" width="90">
              <template #default="{ row }">{{ row.position || '-' }}</template>
            </el-table-column>
            <el-table-column prop="role" label="역할" width="90" />
            <el-table-column label="상태" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="100" align="right">
              <template #default="{ row }">
                <el-button size="small" @click="toggleStatus(row)">
                  {{ row.status === 'ACTIVE' ? '정지' : '활성화' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="`가입 승인 대기 (${signupStore.requests.length})`" name="pending">
        <el-empty v-if="signupStore.requests.length === 0" description="대기 중인 가입 신청이 없습니다." />

        <el-card v-for="r in signupStore.requests" :key="r.id" shadow="never" class="request-card">
          <div class="request">
            <div class="info">
              <strong>{{ r.name }}</strong>
              <span class="email">{{ r.email }}</span>
              <span v-if="r.companyName" class="meta">{{ r.companyName }}</span>
              <span v-if="r.department || r.position" class="meta">
                {{ r.department }}<template v-if="r.department && r.position"> · </template>{{ r.position }}
              </span>
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
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-header h1 {
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
.meta {
  font-size: 12px;
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
