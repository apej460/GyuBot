import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useAdminDocumentsStore = defineStore('adminDocuments', () => {
  const documents = ref([])
  const error = ref('')

  async function fetchList() {
    const { data } = await http.get('/api/documents')
    documents.value = data
    return data
  }

  async function upload({ title, version, category, effectiveDate, revisionDate, file }) {
    error.value = ''
    try {
      const formData = new FormData()
      formData.append('title', title)
      if (version) formData.append('version', version)
      if (category) formData.append('category', category)
      if (effectiveDate) formData.append('effectiveDate', effectiveDate)
      if (revisionDate) formData.append('revisionDate', revisionDate)
      formData.append('file', file)
      await http.post('/api/documents', formData)
    } catch (e) {
      error.value = e.response?.data?.message ?? '문서 등록에 실패했습니다.'
      throw e
    }
  }

  async function updateTitle(id, title) {
    await http.patch(`/api/documents/${id}`, { title })
    await fetchList()
  }

  async function remove(id) {
    await http.delete(`/api/documents/${id}`)
    await fetchList()
  }

  return { documents, error, fetchList, upload, updateTitle, remove }
})
