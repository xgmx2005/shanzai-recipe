<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldCheck } from '@lucide/vue'
import { useMessage } from 'naive-ui'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const form = reactive({
  username: 'user1',
  password: '123456',
})
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const username = form.username.trim()
    const password = form.password
    if (!username || !password) {
      throw new Error('请填写账号和密码')
    }

    const user = await auth.login(username, password)
    message.success('登录成功')
    await router.push(user.role === 'MAINTAINER' ? '/admin/dashboard' : '/user/home')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

function fillDemo(username: string) {
  form.username = username
  form.password = '123456'
}
</script>

<template>
  <AuthShell
    eyebrow="欢迎回来"
    title="登录膳哉"
    description="回到你的健康档案、推荐历史和下一餐计划。维护员也从这里进入数据工作台。"
  >
    <n-alert v-if="error" type="error" :bordered="false">
      {{ error }}
    </n-alert>

    <n-form class="auth-form" @submit.prevent="submit">
      <n-form-item label="账号">
        <n-input v-model:value="form.username" placeholder="请输入用户名" :input-props="{ autocomplete: 'username' }" />
      </n-form-item>
      <n-form-item label="密码">
        <n-input
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          placeholder="请输入密码"
          :input-props="{ autocomplete: 'current-password' }"
        />
      </n-form-item>
      <n-button block size="large" type="primary" :loading="loading" @click="submit">登录</n-button>
    </n-form>

    <div class="demo-box">
      <span><ShieldCheck /> 演示账号</span>
      <button type="button" @click="fillDemo('user1')">user1 / 123456</button>
      <button type="button" @click="fillDemo('maintainer')">maintainer / 123456</button>
    </div>

    <p class="switch-link">
      还没有账号？
      <router-link to="/register">创建膳哉账号</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.auth-form {
  display: grid;
  gap: 2px;
}

.demo-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 18px;
  border-top: 1px solid var(--sz-line);
}

.demo-box span,
.demo-box button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 13px;
  border-radius: var(--sz-radius-pill);
  font-size: 13px;
}

.demo-box span {
  color: var(--sz-deep-green);
  background: var(--sz-mint);
  font-weight: 800;
}

.demo-box span svg {
  width: 15px;
  height: 15px;
}

.demo-box button {
  border: 1px solid var(--sz-line);
  color: var(--sz-text);
  background: var(--sz-surface);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    color 0.18s ease;
}

.demo-box button:hover {
  border-color: var(--sz-green);
  color: var(--sz-deep-green);
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
