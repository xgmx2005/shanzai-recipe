<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bot, MessageCircleMore, RotateCcw, Sparkles } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import {
  confirmConversation,
  getActiveConversation,
  getConversation,
  patchConversationContext,
  sendConversationMessage,
  startConversation,
} from '@/api/recommendation'
import RecommendationComposer from '@/components/recommendation/RecommendationComposer.vue'
import RecommendationConditionSummary from '@/components/recommendation/RecommendationConditionSummary.vue'
import RecommendationMessageList from '@/components/recommendation/RecommendationMessageList.vue'
import { useAuthStore } from '@/stores/auth'
import type { ConversationContextPatchRequest, ConversationMessage, ConversationResponse } from '@/types'
import { recommendationResultRoute } from '@/utils/recommendationConversation'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const loading = ref(true)
const sending = ref(false)
const confirming = ref(false)
const savingConditions = ref(false)
const error = ref('')
const conversation = ref<ConversationResponse | null>(null)
const resumableConversation = ref<ConversationResponse | null>(null)
const showResumeChoice = ref(false)
const pendingUserMessage = ref<ConversationMessage | null>(null)
const streamingAssistantMessage = ref<ConversationMessage | null>(null)
const streamingAssistantContent = ref('')
const agentThinking = computed(() => sending.value && !streamingAssistantMessage.value)
const userAvatarText = computed(() => (auth.user?.nickname ?? auth.user?.username ?? '我').slice(0, 1))
const canGenerateRecommendation = computed(() => {
  const current = conversation.value
  if (!current) return false
  return current.status === 'READY_TO_CONFIRM'
    && current.showConfirmation
    && current.context.unknownTerms.length === 0
    && current.context.conflicts.length === 0
})

const displayMessages = computed(() => {
  const messages = [...(conversation.value?.messages ?? [])]
  if (
    pendingUserMessage.value &&
    !messages.some((item) => item.clientMessageId === pendingUserMessage.value?.clientMessageId)
  ) {
    messages.push(pendingUserMessage.value)
  }

  if (streamingAssistantMessage.value) {
    return messages.map((item) =>
      item.id === streamingAssistantMessage.value?.id
        ? { ...item, content: streamingAssistantContent.value || ' ' }
        : item,
    )
  }

  return messages
})

const contextReadyCount = computed(() => {
  const context = conversation.value?.context
  if (!context) return 0
  return [
    context.dietGoal,
    context.availableIngredients.length > 0,
    context.cookingTime,
    context.servings,
    context.restrictionsConfirmed,
  ].filter(Boolean).length
})

async function syncRoute(id: number) {
  if (route.query.conversationId === String(id)) return
  await router.replace({
    path: '/user/recommend',
    query: { conversationId: String(id) },
  })
}

async function loadConversation(id: number) {
  conversation.value = await getConversation(id)
  await syncRoute(id)
}

async function beginConversation(replaceActive = false) {
  error.value = ''
  loading.value = true
  showResumeChoice.value = false
  resetTransientMessages()
  try {
    conversation.value = await startConversation(replaceActive)
    await syncRoute(conversation.value.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建推荐对话失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

async function initialize() {
  error.value = ''
  loading.value = true
  try {
    const queryId = Number(route.query.conversationId)
    if (Number.isFinite(queryId) && queryId > 0) {
      await loadConversation(queryId)
      return
    }

    const active = await getActiveConversation()
    if (active) {
      resumableConversation.value = active
      showResumeChoice.value = true
      conversation.value = null
      return
    }

    await beginConversation(false)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐对话加载失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

async function continuePrevious() {
  if (!resumableConversation.value) return
  conversation.value = resumableConversation.value
  showResumeChoice.value = false
  await syncRoute(conversation.value.id)
}

async function restart() {
  await beginConversation(true)
}

function resetTransientMessages() {
  pendingUserMessage.value = null
  streamingAssistantMessage.value = null
  streamingAssistantContent.value = ''
}

function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function streamAssistantContent(content: string) {
  const chars = Array.from(content)
  const chunkSize = chars.length > 220 ? 4 : chars.length > 120 ? 3 : 2
  for (let index = 0; index < chars.length; index += chunkSize) {
    streamingAssistantContent.value = chars.slice(0, index + chunkSize).join('')
    await sleep(18)
  }
}

async function submitMessage(content: string) {
  if (sending.value) return
  if (!conversation.value) {
    await beginConversation(false)
  }
  if (!conversation.value) return

  sending.value = true
  error.value = ''
  const previousMessageIds = new Set(conversation.value.messages.map((item) => item.id))
  const clientMessageId = `msg-${Date.now()}-${Math.random().toString(16).slice(2)}`
  pendingUserMessage.value = {
    id: -Date.now(),
    role: 'USER',
    content,
    clientMessageId,
    createdAt: new Date().toISOString(),
  }
  try {
    const nextConversation = await sendConversationMessage(conversation.value.id, {
      content,
      clientMessageId,
    })
    const assistantMessage = nextConversation.messages.find(
      (item) => item.role === 'ASSISTANT' && !previousMessageIds.has(item.id),
    )
    conversation.value = nextConversation
    pendingUserMessage.value = null
    await syncRoute(conversation.value.id)
    if (assistantMessage?.content) {
      streamingAssistantMessage.value = assistantMessage
      streamingAssistantContent.value = ''
      await streamAssistantContent(assistantMessage.content)
    }
  } catch (err) {
    resetTransientMessages()
    error.value = err instanceof Error ? err.message : '发送失败，请稍后重试'
    message.error(error.value)
  } finally {
    streamingAssistantMessage.value = null
    streamingAssistantContent.value = ''
    sending.value = false
  }
}

async function confirm() {
  if (!conversation.value || confirming.value) return
  confirming.value = true
  error.value = ''
  try {
    const result = await confirmConversation(conversation.value.id)
    message.success('推荐已生成')
    await router.push(recommendationResultRoute(result.historyId))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成推荐失败'
    message.error(error.value)
  } finally {
    confirming.value = false
  }
}

async function saveConditions(payload: ConversationContextPatchRequest) {
  if (!conversation.value || savingConditions.value) return
  savingConditions.value = true
  error.value = ''
  try {
    conversation.value = await patchConversationContext(conversation.value.id, payload)
    message.success('条件已更新')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '条件保存失败'
    message.error(error.value)
  } finally {
    savingConditions.value = false
  }
}

onMounted(initialize)
</script>

<template>
  <div class="recommend-view">
    <section class="recommend-heading">
      <div>
        <p class="sz-chip"><Sparkles :size="15" /> 智能推荐</p>
        <h1>像聊天一样说清楚这一餐</h1>
      </div>
      <p>膳哉会先理解食材、口味、忌口和人数，再结合知识库生成可执行的推荐结果。</p>
    </section>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>

    <section v-if="showResumeChoice && resumableConversation" class="resume-panel sz-panel">
      <div>
        <p class="sz-chip is-warm"><MessageCircleMore :size="15" /> 未完成对话</p>
        <h2>继续上一次推荐，还是重新开始？</h2>
        <span>已记录 {{ resumableConversation.messages.length }} 条消息，可继续补充条件。</span>
      </div>
      <div class="resume-actions">
        <button type="button" class="secondary-button" @click="restart">
          <RotateCcw :size="17" />
          重新开始
        </button>
        <button type="button" class="primary-button" @click="continuePrevious">
          继续上次
        </button>
      </div>
    </section>

    <section v-else class="conversation-shell sz-panel">
      <div class="conversation-status">
        <div>
          <p class="sz-chip"><MessageCircleMore :size="15" /> 对话输入</p>
          <h2>{{ conversation ? '告诉我你今天想吃什么' : '正在准备推荐助手' }}</h2>
        </div>
        <span>{{ contextReadyCount }}/5 条件已明确</span>
      </div>

      <n-skeleton v-if="loading && !conversation" text :repeat="5" />

      <template v-else-if="conversation">
        <div class="conversation-workspace">
          <div class="conversation-main">
            <div v-if="displayMessages.length === 0" class="agent-prompt">
              <span class="agent-orb"><Bot :size="23" /></span>
              <strong>先说说你今天想怎么吃</strong>
              <p>例如：家里有鸡蛋、西兰花和牛肉，想吃清淡一点，20 分钟内做好。</p>
            </div>

            <RecommendationMessageList
              v-else
              :messages="displayMessages"
              :loading="agentThinking || confirming"
              :streaming-message-id="streamingAssistantMessage?.id ?? null"
              :user-avatar-text="userAvatarText"
              :user-avatar-url="auth.user?.avatarUrl ?? ''"
              :show-generate-action="canGenerateRecommendation"
              :generating="confirming"
              @generate="confirm"
            />

            <RecommendationComposer
              :quick-options="conversation.quickOptions"
              :loading="sending || confirming"
              @submit="submitMessage"
              @restart="restart"
            />
          </div>

          <RecommendationConditionSummary
            class="condition-dock"
            :context="conversation.context"
            :status="conversation.status"
            :show-confirmation="conversation.showConfirmation"
            :confirming="confirming"
            :saving="savingConditions"
            @confirm="confirm"
            @save="saveConditions"
          />
        </div>
      </template>

      <div v-else class="empty-state">
        <Sparkles :size="24" />
        <strong>推荐助手暂时没有响应</strong>
        <button type="button" class="primary-button" @click="beginConversation(false)">重新加载</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.recommend-view {
  display: grid;
  gap: 20px;
}

h1,
h2,
p {
  margin: 0;
}

.recommend-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.recommend-heading > div {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.recommend-heading h1 {
  color: var(--sz-evergreen);
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: 0;
}

.recommend-heading > p {
  max-width: 450px;
  color: var(--sz-muted);
  line-height: 1.8;
  text-align: right;
}

.conversation-shell,
.resume-panel {
  max-width: 880px;
  width: 100%;
  justify-self: center;
}

.conversation-shell {
  display: grid;
  gap: 15px;
  max-width: 1080px;
  padding: 22px;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(251, 247, 239, 0.94));
}

.conversation-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 16px;
  align-items: start;
}

.conversation-main {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.agent-prompt {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 230px;
  padding: 34px 28px;
  border: 1px dashed rgba(35, 107, 75, 0.24);
  border-radius: 20px;
  color: var(--sz-muted);
  background:
    radial-gradient(circle at 50% 38%, rgba(72, 168, 106, 0.13), transparent 34%),
    rgba(220, 239, 228, 0.34);
  text-align: center;
}

.agent-orb {
  position: relative;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  box-shadow: 0 10px 22px rgba(35, 107, 75, 0.14);
  animation: agent-float 2.2s ease-in-out infinite;
}

.agent-orb::after {
  position: absolute;
  inset: -10px;
  border: 1px solid rgba(35, 107, 75, 0.14);
  border-radius: inherit;
  content: '';
  animation: agent-pulse 2.2s ease-in-out infinite;
}

.agent-prompt strong {
  color: var(--sz-evergreen);
  font-size: 20px;
}

.agent-prompt p {
  max-width: 420px;
  color: var(--sz-muted);
  line-height: 1.7;
}

@keyframes agent-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}

@keyframes agent-pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.92);
  }

  50% {
    opacity: 0.95;
    transform: scale(1.04);
  }
}

.conversation-status,
.resume-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.conversation-status > div,
.resume-panel > div:first-child {
  display: grid;
  justify-items: start;
  gap: 9px;
}

.conversation-status h2,
.resume-panel h2 {
  color: var(--sz-evergreen);
  font-size: 24px;
}

.conversation-status > span,
.resume-panel span {
  color: var(--sz-muted);
  font-weight: 800;
}

.resume-panel {
  padding: 24px;
}

.resume-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  font-weight: 900;
  cursor: pointer;
}

.primary-button {
  border: 0;
  color: #ffffff;
  background: var(--sz-green-dark);
  box-shadow: 0 10px 18px rgba(35, 107, 75, 0.18);
}

.secondary-button {
  border: 1px solid rgba(35, 107, 75, 0.16);
  color: var(--sz-deep-green);
  background: var(--sz-mint);
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 12px;
  min-height: 240px;
  padding: 26px;
  border: 1px dashed rgba(35, 107, 75, 0.24);
  border-radius: 18px;
  color: var(--sz-muted);
  background: rgba(220, 239, 228, 0.34);
  text-align: center;
}

.empty-state svg {
  color: var(--sz-green-dark);
}

.empty-state strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

@media (max-width: 760px) {
  .recommend-heading,
  .conversation-status,
  .resume-panel {
    display: grid;
  }

  .recommend-heading > p {
    max-width: none;
    text-align: left;
  }

  .conversation-shell,
  .resume-panel {
    padding: 18px;
  }

  .conversation-workspace {
    grid-template-columns: 1fr;
  }

  .agent-prompt {
    min-height: 210px;
  }

  .resume-actions,
  .primary-button,
  .secondary-button {
    width: 100%;
  }
}
</style>
