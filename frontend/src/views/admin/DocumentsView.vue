<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAdminDocumentsStore } from '@/stores/adminDocuments'

const store = useAdminDocumentsStore()
const router = useRouter()
const keyword = ref('')

onMounted(() => {
  store.fetchList()
})

const filteredDocuments = computed(() => {
  if (!keyword.value) return store.documents
  const q = keyword.value.toLowerCase()
  return store.documents.filter(
    (doc) => doc.title.toLowerCase().includes(q) || (doc.category ?? '').toLowerCase().includes(q),
  )
})

function statusLabel(status) {
  return status === 'UPCOMING' ? '시행 예정' : '사용 중'
}

async function handleDelete(doc) {
  await ElMessageBox.confirm(`"${doc.title}" 문서를 삭제할까요?`, '문서 삭제', {
    type: 'warning',
    confirmButtonText: '삭제',
    cancelButtonText: '취소',
  })
  await store.remove(doc.id)
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>문서 관리</h1>
      <el-button type="primary" @click="router.push({ name: 'admin-documents-new' })">문서 등록</el-button>
    </div>

    <el-input v-model="keyword" placeholder="제목 또는 분류 검색" clearable class="keyword-input" />

    <el-card shadow="never">
      <el-table :data="filteredDocuments" style="width: 100%">
        <el-table-column prop="title" label="제목" />
        <el-table-column prop="version" label="버전" width="100">
          <template #default="{ row }">{{ row.version || '-' }}</template>
        </el-table-column>
        <el-table-column prop="category" label="분류" width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column prop="effectiveDate" label="시행일" width="120">
          <template #default="{ row }">{{ row.effectiveDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="상태" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UPCOMING' ? 'warning' : 'success'" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="" width="140" align="right">
          <template #default="{ row }">
            <el-button size="small" tag="a" :href="`/api/documents/${row.id}/download`" target="_blank">
              다운로드
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">삭제</el-button>
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
  gap: 16px;
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
.keyword-input {
  max-width: 320px;
}
</style>
