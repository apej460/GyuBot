<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminDocumentsStore } from '@/stores/adminDocuments'

const store = useAdminDocumentsStore()
const router = useRouter()

const title = ref('')
const version = ref('')
const category = ref('')
const effectiveDate = ref('')
const revisionDate = ref('')
const file = ref(null)
const submitting = ref(false)

function handleFileChange(uploadFile) {
  file.value = uploadFile.raw
}

async function submitUpload() {
  submitting.value = true
  try {
    await store.upload({
      title: title.value,
      version: version.value,
      category: category.value,
      effectiveDate: effectiveDate.value,
      revisionDate: revisionDate.value,
      file: file.value,
    })
    router.push({ name: 'admin-documents' })
  } catch {
    // store.error에 메시지가 담겨 템플릿에 표시됨
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1>문서 등록</h1>

    <el-card shadow="never" class="form-card">
      <el-form label-position="top" @submit.prevent="submitUpload">
        <el-form-item label="제목">
          <el-input v-model="title" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="버전">
            <el-input v-model="version" placeholder="예: v1.2" />
          </el-form-item>
          <el-form-item label="분류">
            <el-input v-model="category" placeholder="예: 인사 규정" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="시행일">
            <el-date-picker v-model="effectiveDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
          <el-form-item label="개정일">
            <el-date-picker v-model="revisionDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="파일 (PDF, HWP / 최대 50MB)">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.hwp"
            :on-change="handleFileChange"
            class="upload"
          >
            <p class="upload-text">파일을 드래그하거나 클릭해서 첨부하세요</p>
          </el-upload>
        </el-form-item>

        <el-button type="primary" native-type="submit" :loading="submitting" :disabled="!title || !file">
          등록
        </el-button>
        <el-button @click="router.push({ name: 'admin-documents' })">취소</el-button>
      </el-form>

      <el-alert v-if="store.error" :title="store.error" type="error" show-icon :closable="false" class="alert" />
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
.form-card {
  max-width: 560px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.full-width {
  width: 100%;
}
.upload {
  width: 100%;
}
.upload-text {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.alert {
  margin-top: 12px;
}
</style>
