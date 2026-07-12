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
  username: '',
  password: '',
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
