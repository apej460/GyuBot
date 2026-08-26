<script setup>
import { onMounted, ref } from 'vue'
import { useCompanyStore } from '@/stores/company'

const store = useCompanyStore()
const dialogOpen = ref(false)
const name = ref('')
const emailDomain = ref('')
const submitting = ref(false)

onMounted(() => {
  store.fetchList()
})

function openDialog() {
  name.value = ''
  emailDomain.value = ''
  store.error = ''
  dialogOpen.value = true
}

async function submitRegister() {
  submitting.value = true
  try {
    await store.register({ name: name.value, emailDomain: emailDomain.value })
    dialogOpen.value = false
  } catch {
    // store.error에 메시지가 담겨 다이얼로그에 표시됨
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>시스템 관리</h1>
      <el-button type="primary" @click="openDialog">회사 등록</el-button>
    </div>
    <p class="hint">가입 도메인이 등록된 회사만 일반 가입(이메일 인증) 화면에서 가입할 수 있습니다.</p>

    <el-card shadow="never">
      <el-table :data="store.companies" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="회사명" />
        <el-table-column prop="emailDomain" label="이메일 도메인" width="180" />
        <el-table-column prop="tenantCode" label="테넌트 코드" width="140" />
        <el-table-column label="상태" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogOpen" title="회사 등록" width="400px">
      <el-form label-position="top" @submit.prevent="submitRegister">
        <el-form-item label="회사명">
          <el-input v-model="name" />
        </el-form-item>
        <el-form-item label="이메일 도메인">
          <el-input v-model="emailDomain" placeholder="예: gyubot.local" />
        </el-form-item>
      </el-form>

      <el-alert v-if="store.error" :title="store.error" type="error" show-icon :closable="false" class="alert" />

      <template #footer>
        <el-button @click="dialogOpen = false">취소</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRegister">등록</el-button>
      </template>
    </el-dialog>
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
  justify-content: space-between;
}
.page-header h1 {
  margin: 0;
  font-size: 22px;
}
.hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: #909399;
}
.alert {
  margin-top: 12px;
}
</style>
