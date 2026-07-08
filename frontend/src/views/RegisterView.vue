<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const form = reactive({
  nickname: '',
  username: '',
  password: '',
  confirmPassword: '',
})
const loading = ref(false)
const error = ref('')

function validateForm() {
  const nickname = form.nickname.trim()
  const username = form.username.trim()
  const password = form.password

  if (!nickname || !username || !password || !form.confirmPassword) {
    throw new Error('请完整填写注册信息')
  }
  if (password.length < 6) {
    throw new Error('密码至少 6 位')
  }
  if (password !== form.confirmPassword) {
    throw new Error('两次输入的密码不一致')
  }

  return { nickname, username, password }
}

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const payload = validateForm()
    await auth.register(payload.username, payload.password, payload.nickname)
    message.success('注册成功')
    await router.push('/user/home')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell
    eyebrow="开始使用"
    title="创建膳哉账号"
    description="先建立一个账号，后续你的健康档案、收藏菜谱和购物清单都会被保存下来。"
    image-url="https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=85"
  >
    <n-alert v-if="error" type="error" :bordered="false">
      {{ error }}
    </n-alert>

    <n-form class="auth-form" @submit.prevent="submit">
      <n-form-item label="昵称">
        <n-input v-model:value="form.nickname" placeholder="例如：轻食用户" />
      </n-form-item>
      <n-form-item label="账号">
        <n-input v-model:value="form.username" placeholder="请输入用户名" :input-props="{ autocomplete: 'username' }" />
      </n-form-item>
      <n-form-item label="密码">
        <n-input
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          placeholder="至少 6 位"
          :input-props="{ autocomplete: 'new-password' }"
        />
      </n-form-item>
      <n-form-item label="确认密码">
        <n-input
          v-model:value="form.confirmPassword"
          type="password"
          show-password-on="click"
          placeholder="再次输入密码"
          :input-props="{ autocomplete: 'new-password' }"
        />
      </n-form-item>
      <n-button block size="large" type="primary" :loading="loading" @click="submit">注册并进入</n-button>
    </n-form>

    <p class="switch-link">
      已经有账号？
      <router-link to="/login">返回登录</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.auth-form {
  display: grid;
  gap: 2px;
}

.switch-link {
  margin: 0;
  color: var(--sz-muted);
  text-align: center;
}

.switch-link a {
  color: var(--sz-green-dark);
  font-weight: 900;
}
</style>
