<script setup lang="ts">
import { Bot } from '@lucide/vue'
import { backendAssetUrl } from '@/api/http'
import type { ConversationMessage } from '@/types'

withDefaults(defineProps<{
  messages: ConversationMessage[]
  loading?: boolean
  userAvatarText?: string
  userAvatarUrl?: string
}>(), {
  userAvatarText: '我',
  userAvatarUrl: '',
})

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <section class="message-list" aria-label="推荐对话记录">
    <div v-if="messages.length === 0" class="empty-thread">
      <Bot :size="24" />
      <strong>先说说你今天想怎么吃</strong>
      <span>例如：家里有鸡蛋、西兰花和牛肉，想吃清淡一点，20 分钟内做好。</span>
    </div>

    <article
      v-for="item in messages"
      :key="item.id"
      class="message-item"
      :class="{ 'is-user': item.role === 'USER' }"
    >
      <span class="avatar">
        <img v-if="item.role === 'USER' && userAvatarUrl" :src="backendAssetUrl(userAvatarUrl)" alt="" />
        <template v-else-if="item.role === 'USER'">{{ userAvatarText }}</template>
        <Bot v-else :size="17" />
      </span>
      <div class="bubble">
        <p>{{ item.content }}</p>
        <time>{{ formatTime(item.createdAt) }}</time>
      </div>
    </article>

    <article v-if="loading" class="message-item">
      <span class="avatar"><Bot :size="17" /></span>
      <div class="bubble is-thinking">
        <span />
        <span />
        <span />
      </div>
    </article>
  </section>
</template>

<style scoped>
.message-list {
  display: grid;
  gap: 14px;
}

.empty-thread {
  display: grid;
  justify-items: center;
  gap: 9px;
  min-height: 210px;
  padding: 28px;
  border: 1px dashed rgba(35, 107, 75, 0.24);
  border-radius: 18px;
  color: var(--sz-muted);
  background: rgba(220, 239, 228, 0.34);
  text-align: center;
}

.empty-thread svg {
  color: var(--sz-green-dark);
}

.empty-thread strong {
  color: var(--sz-evergreen);
  font-size: 18px;
}

.empty-thread span {
  max-width: 420px;
  line-height: 1.7;
}

.message-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.message-item.is-user {
  grid-template-columns: minmax(0, 1fr) 36px;
}

.message-item.is-user .avatar {
  grid-column: 2;
  color: #ffffff;
  background: var(--sz-green-dark);
}

.message-item.is-user .bubble {
  grid-column: 1;
  grid-row: 1;
  justify-self: end;
  color: #ffffff;
  background: var(--sz-green-dark);
}

.message-item.is-user time {
  color: rgba(255, 255, 255, 0.76);
}

.avatar {
  overflow: hidden;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-size: 13px;
  font-weight: 900;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bubble {
  display: grid;
  gap: 8px;
  max-width: 640px;
  padding: 13px 15px;
  border: 1px solid rgba(223, 210, 191, 0.8);
  border-radius: 16px;
  color: var(--sz-text);
  background: rgba(255, 253, 247, 0.92);
  box-shadow: 0 8px 18px rgba(31, 77, 58, 0.06);
}

.bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.75;
}

.bubble time {
  color: var(--sz-muted);
  font-size: 12px;
  font-weight: 800;
}

.bubble.is-thinking {
  display: inline-flex;
  width: max-content;
  gap: 5px;
}

.bubble.is-thinking span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--sz-green-dark);
  opacity: 0.55;
}

@media (max-width: 640px) {
  .message-item,
  .message-item.is-user {
    grid-template-columns: 32px minmax(0, 1fr);
  }

  .message-item.is-user .avatar {
    grid-column: 1;
  }

  .message-item.is-user .bubble {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
