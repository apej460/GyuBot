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
</script>

<template>
  <div class="page">
    <nav>
      <RouterLink to="/">← 홈</RouterLink>
      <RouterLink to="/chat/history">질의 이력</RouterLink>
      <RouterLink to="/chat" @click="chatStore.startNewSession()">새 대화</RouterLink>
    </nav>
    <h1>AI 질의</h1>

    <div class="messages">
      <p v-if="chatStore.messages.length === 0 && !chatStore.loadingHistory" class="empty">
        궁금한 사내 규정을 물어보세요.
      </p>
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
              {{ source.originalFilename }}
              <a :href="`/api/documents/${source.documentId}/download`" target="_blank">원문 다운로드</a>
            </p>
            <p class="source-snippet">{{ source.snippet }}</p>
          </div>
        </div>
      </div>
      <p v-if="chatStore.asking" class="thinking">답변을 생각하는 중…</p>
    </div>

    <p v-if="chatStore.error" class="error">{{ chatStore.error }}</p>

    <form class="ask-form" @submit.prevent="submitQuestion">
      <textarea
        v-model="question"
        placeholder="예: 연차 휴가는 며칠까지 쓸 수 있어?"
        rows="2"
        :disabled="chatStore.asking"
        @keydown.enter.exact.prevent="submitQuestion"
      />
      <button type="submit" :disabled="chatStore.asking || !question.trim()">전송</button>
    </form>
  </div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 40px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 16px;
}
nav {
  display: flex;
  gap: 12px;
}
.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}
.empty {
  color: #999;
  text-align: center;
  margin-top: 40px;
}
.message {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
}
.message.user {
  align-self: flex-end;
  background: #dbeafe;
}
.message.assistant {
  align-self: flex-start;
  background: #f1f1f1;
}
.content {
  margin: 0;
  white-space: pre-wrap;
}
.sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sources-title {
  margin: 0;
  font-size: 12px;
  font-weight: bold;
  color: #666;
}
.source {
  font-size: 12px;
  color: #555;
}
.source-file {
  margin: 0;
  font-weight: bold;
}
.source-file a {
  margin-left: 8px;
  font-weight: normal;
  font-size: 11px;
}
.source-snippet {
  margin: 2px 0 0;
  color: #777;
}
.thinking {
  align-self: flex-start;
  color: #999;
  font-size: 14px;
}
.error {
  color: #c0392b;
}
.ask-form {
  display: flex;
  gap: 8px;
}
textarea {
  flex: 1;
  padding: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
}
button {
  padding: 0 20px;
  cursor: pointer;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
