<script setup>
import { computed, onMounted, ref } from 'vue'
import { useManagerStore } from '@/stores/manager'
import { useAuthStore } from '@/stores/auth'

const store = useManagerStore()
const auth = useAuthStore()
const isSuperAdmin = computed(() => auth.user?.role === 'SUPER_ADMIN')

const dialogOpen = ref(false)
const email = ref('')
const name = ref('')
const password = ref('')
const role = ref('ADMIN')
const submitting = ref(false)

onMounted(() => {
  store.fetchList()
})

function openDialog() {
  email.value = ''
  name.value = ''
  password.value = ''
  role.value = 'ADMIN'
  store.error = ''
  dialogOpen.value = true
}

async function submitCreate() {
  submitting.value = true
  try {
    await store.create({ email: email.value, name: name.value, password: password.value, role: role.value })
    dialogOpen.value = false
  } catch {
    // store.error에 메시지가 담겨 다이얼로그에 표시됨
  } finally {
    submitting.value = false
  }
}

function roleLabel(r) {
  return r === 'SUPER_ADMIN' ? '최고관리자' : '관리자'
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>관리자 관리</h1>
      <el-button type="primary" @click="openDialog">관리자 추가</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="store.managers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="이름" width="140" />
        <el-table-column prop="email" label="이메일" />
        <el-table-column v-if="isSuperAdmin" prop="companyId" label="회사 ID" width="90" />
        <el-table-column label="역할" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'SUPER_ADMIN' ? 'warning' : ''" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogOpen" title="관리자 추가" width="400px">
      <el-form label-position="top" @submit.prevent="submitCreate">
        <el-form-item label="이메일">
          <el-input v-model="email" type="email" />
        </el-form-item>
        <el-form-item label="이름">
          <el-input v-model="name" />
        </el-form-item>
        <el-form-item label="비밀번호 (8자 이상)">
          <el-input v-model="password" type="password" show-password minlength="8" />
        </el-form-item>
        <el-form-item label="역할">
          <el-select v-model="role" class="full-width">
            <el-option label="관리자" value="ADMIN" />
            <el-option v-if="isSuperAdmin" label="최고관리자" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-alert v-if="store.error" :title="store.error" type="error" show-icon :closable="false" class="alert" />

      <template #footer>
        <el-button @click="dialogOpen = false">취소</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">추가</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
.full-width {
  width: 100%;
}
.alert {
  margin-top: 12px;
}
</style>
