<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const question = ref('')
const drawerOpen = ref(false)
const drawerLoading = ref(false)
const drawerDoc = ref(null)
const drawerSnippet = ref('')

const exampleQuestions = ['연차 휴가는 며칠까지 쓸 수 있어?', '출장비 정산은 어떻게 해?', '재택근무 신청 절차가 궁금해']

async function loadFromRoute() {
  if (route.params.id) {
    await chatStore.loadSession(route.params.id)
  } else {
    chatStore.startNewSession()
  }
}

onMounted(loadFromRoute)
watch(() => route.params.id, loadFromRoute)

async function ask(text) {
  if (!text || chatStore.asking) return
  question.value = ''
  const wasNewSession = !chatStore.currentSessionId
  await chatStore.ask(text).catch(() => {})
  if (wasNewSession && chatStore.currentSessionId) {
    router.replace({ name: 'chat', params: { id: chatStore.currentSessionId } })
  }
}

function submitQuestion() {
  ask(question.value.trim())
}

function startNewChat() {
  chatStore.startNewSession()
  if (route.params.id) router.push({ name: 'chat' })
}

async function openCitation(source) {
  drawerOpen.value = true
  drawerLoading.value = true
  drawerDoc.value = null
  drawerSnippet.value = source.snippet
  try {
    drawerDoc.value = await chatStore.fetchCitation(source.documentId)
  } finally {
    drawerLoading.value = false
  }
}

function statusLabel(status) {
  return status === 'UPCOMING' ? '시행 예정' : '사용 중'
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>AI 질의</h1>
      <el-button size="small" @click="startNewChat">+ 새 대화</el-button>
    </div>

    <el-card shadow="never" class="chat-card">
      <div class="messages">
        <div v-if="chatStore.messages.length === 0 && !chatStore.loadingHistory" class="landing">
          <p class="landing-title">무엇이든 물어보세요.</p>
          <p class="landing-subtitle">사내 규정·문서를 근거로 AI가 답변해드립니다.</p>
          <div class="examples">
            <el-tag
              v-for="q in exampleQuestions"
              :key="q"
              class="example-chip"
              @click="ask(q)"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <div
          v-for="message in chatStore.messages"
          :key="message.id"
          class="message"
          :class="message.role.toLowerCase()"
        >
          <p class="content">{{ message.content }}</p>
          <div v-if="message.sources?.length" class="sources">
            <p class="sources-title">근거 문서 {{ message.sources.length }}개 문서에서 답변을 확인했어요</p>
            <button
              v-for="(source, i) in message.sources"
              :key="i"
              type="button"
              class="source-card"
              @click="openCitation(source)"
            >
              <span class="source-file">{{ source.originalFilename }}</span>
              <span class="source-snippet">{{ source.snippet }}</span>
            </button>
          </div>
        </div>

        <div v-if="chatStore.asking" class="thinking">
          <span class="dot" /><span class="dot" /><span class="dot" />
          답변을 생각하는 중…
        </div>
      </div>

      <el-alert v-if="chatStore.error" :title="chatStore.error" type="error" show-icon :closable="false" class="alert" />

      <form class="ask-form" @submit.prevent="submitQuestion">
        <el-input
          v-model="question"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="예: 연차 휴가는 며칠까지 쓸 수 있어?"
          :disabled="chatStore.asking"
          @keydown.enter.exact.prevent="submitQuestion"
        />
        <el-button type="primary" native-type="submit" :loading="chatStore.asking" :disabled="!question.trim()">
          전송
        </el-button>
      </form>
    </el-card>

    <el-drawer v-model="drawerOpen" title="근거 문서" size="380px">
      <el-skeleton v-if="drawerLoading" :rows="5" animated />
      <template v-else-if="drawerDoc">
        <h3 class="drawer-title">{{ drawerDoc.title }}</h3>
        <el-descriptions :column="1" border size="small" class="drawer-meta">
          <el-descriptions-item label="버전">{{ drawerDoc.version || '-' }}</el-descriptions-item>
          <el-descriptions-item label="분류">{{ drawerDoc.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="시행일">{{ drawerDoc.effectiveDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="개정일">{{ drawerDoc.revisionDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="상태">
            <el-tag :type="drawerDoc.status === 'UPCOMING' ? 'warning' : 'success'" size="small">
              {{ statusLabel(drawerDoc.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <p class="drawer-excerpt-title">발췌 내용</p>
        <p class="drawer-excerpt">{{ drawerSnippet }}</p>

        <el-button
          type="primary"
          tag="a"
          :href="`/api/documents/${drawerDoc.id}/download`"
          target="_blank"
          class="drawer-download"
        >
          원본 문서 다운로드
        </el-button>
      </template>
    </el-drawer>
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
.chat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}
.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 240px;
}
.landing {
  padding: 40px 0;
  text-align: center;
}
.landing-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.landing-subtitle {
  margin: 6px 0 20px;
  color: #909399;
  font-size: 13px;
}
.examples {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}
.example-chip {
  cursor: pointer;
}
.message {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.55;
}
.message.user {
  align-self: flex-end;
  background: #ecf5ff;
  color: #1f2329;
}
.message.assistant {
  align-self: flex-start;
  background: #f5f7fa;
}
.content {
  margin: 0;
  white-space: pre-wrap;
}
.sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sources-title {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
}
.source-card {
  text-align: left;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px 10px;
  background: #fff;
  cursor: pointer;
  font: inherit;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.source-card:hover {
  border-color: var(--el-color-primary);
}
.source-file {
  font-weight: 600;
  font-size: 12px;
  color: #303133;
}
.source-snippet {
  font-size: 12px;
  color: #909399;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.thinking {
  align-self: flex-start;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: pulse 1.2s infinite ease-in-out;
}
.dot:nth-child(2) {
  animation-delay: 0.2s;
}
.dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes pulse {
  0%, 80%, 100% {
    opacity: 0.3;
  }
  40% {
    opacity: 1;
  }
}
.alert {
  margin: 0;
}
.ask-form {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.ask-form :deep(.el-textarea) {
  flex: 1;
}
.drawer-title {
  margin: 0 0 12px;
  font-size: 16px;
}
.drawer-meta {
  margin-bottom: 20px;
}
.drawer-excerpt-title {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
}
.drawer-excerpt {
  margin: 0 0 24px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
}
.drawer-download {
  width: 100%;
}
</style>
