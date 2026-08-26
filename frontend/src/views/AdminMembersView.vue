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
    <h1>회원 관리</h1>

    <el-card shadow="never">
      <el-table :data="memberStore.members" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="이름" width="140" />
        <el-table-column prop="email" label="이메일" />
        <el-table-column prop="role" label="역할" width="100" />
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
</style>
