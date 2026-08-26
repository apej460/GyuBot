<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const question = ref('')

async function loadFromRoute() {
  if (route.params.id) {
    await chatStore.loadSession(route.params.id)
  } else {
    chatStore.startNewSession()
  }
}

onMounted(loadFromRoute)
watch(() => route.params.id, loadFromRoute)

async function submitQuestion() {
  const text = question.value.trim()
  if (!text || chatStore.asking) return
  question.value = ''
  const wasNewSession = !chatStore.currentSessionId
  await chatStore.ask(text).catch(() => {})
  if (wasNewSession && chatStore.currentSessionId) {
    router.replace({ name: 'chat', params: { id: chatStore.currentSessionId } })
  }
}

function startNewChat() {
  chatStore.startNewSession()
  if (route.params.id) router.push({ name: 'chat' })
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
        <el-empty v-if="chatStore.messages.length === 0 && !chatStore.loadingHistory" description="궁금한 사내 규정을 물어보세요." />

        <div
          v-for="message in chatStore.messages"
          :key="message.id"
          class="message"
          :class="message.role.toLowerCase()"
        >
          <p class="content">{{ message.content }}</p>
          <div v-if="message.sources?.length" class="sources">
            <p class="sources-title">근거 문서</p>
            <div v-for="(source, i) in message.sources" :key="i" class="source">
              <p class="source-file">
                <span>{{ source.originalFilename }}</span>
                <a :href="`/api/documents/${source.documentId}/download`" target="_blank">원문 다운로드</a>
              </p>
              <p class="source-snippet">{{ source.snippet }}</p>
            </div>
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
  gap: 10px;
}
.sources-title {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
}
.source {
  font-size: 12px;
  color: #606266;
}
.source-file {
  margin: 0;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.source-file a {
  font-weight: normal;
  font-size: 11px;
}
.source-snippet {
  margin: 4px 0 0;
  color: #909399;
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
</style>
