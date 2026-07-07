<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CheckCircle2, Leaf, ShieldCheck } from '@lucide/vue'
import { useMessage } from 'naive-ui'
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
    await new Promise((resolve) => window.setTimeout(resolve, 450))
    const user = auth.login(form.username.trim(), form.password)
    message.success('登录成功')
    router.push(user.role === 'MAINTAINER' ? '/admin/dashboard' : '/user/home')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
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
  <main class="login-page">
    <section class="login-shell">
      <aside class="brand-panel">
        <div class="logo-row">
          <Leaf />
          <strong>膳哉</strong>
        </div>
        <h1>让今天这一餐，更适合你的身体</h1>
        <p>AI 健康食谱助手，根据档案、目标和现有食材推荐更合适的一餐。</p>
        <ol>
          <li><CheckCircle2 /> 填写健康档案</li>
          <li><CheckCircle2 /> 输入已有食材</li>
          <li><CheckCircle2 /> 获取推荐和购物清单</li>
        </ol>
      </aside>

      <section class="form-panel">
        <p class="eyebrow">01 登录 / 注册</p>
        <h2>登录膳哉</h2>
        <p class="sz-muted">根据你的身份进入个性化推荐或维护工作台。</p>

        <n-alert v-if="error" class="error" type="error" :bordered="false">
          {{ error }}
        </n-alert>

        <n-form @submit.prevent="submit">
          <n-form-item label="账号">
            <n-input v-model:value="form.username" placeholder="请输入手机号 / 邮箱 / 用户名" />
          </n-form-item>
          <n-form-item label="密码">
            <n-input
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
            />
          </n-form-item>
          <div class="form-extra">
            <n-checkbox>记住我</n-checkbox>
            <a href="#">忘记密码？</a>
          </div>
          <n-button block size="large" type="primary" :loading="loading" @click="submit">登录</n-button>
        </n-form>

        <div class="demo-box">
          <span><ShieldCheck /> 演示账号</span>
          <button type="button" @click="fillDemo('user1')">user1 / 123456</button>
          <button type="button" @click="fillDemo('maintainer')">maintainer / 123456</button>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
}

.login-shell {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
  padding: 20px;
  border: 1px solid var(--sz-line);
  border-radius: 28px;
  background: rgba(255, 250, 241, 0.9);
  box-shadow: var(--sz-shadow);
}

.brand-panel {
  position: relative;
  overflow: hidden;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  padding: 34px;
  border-radius: 24px;
  color: #ffffff;
  background-color: var(--sz-evergreen);
  background-image: url('https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=900&q=80');
  background-position: center;
  background-size: cover;
  background-blend-mode: multiply;
}

.brand-panel::after {
  position: absolute;
  inset: 0;
  content: "";
  background: rgba(18, 61, 45, 0.36);
  pointer-events: none;
}

.brand-panel > * {
  position: relative;
  z-index: 1;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-row svg {
  width: 28px;
  height: 28px;
}

.logo-row strong {
  font-size: 40px;
}

h1,
h2,
p,
ol {
  margin: 0;
}

.brand-panel h1 {
  margin-top: 56px;
  font-size: 34px;
  line-height: 1.25;
}

.brand-panel p {
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.8;
}

ol {
  display: grid;
  gap: 12px;
  margin-top: auto;
  padding: 0;
  list-style: none;
}

li {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(255, 250, 241, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-weight: 800;
}

li svg {
  width: 18px;
  height: 18px;
}

.form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 56px;
}

.eyebrow {
  color: var(--sz-deep-green);
  font-weight: 900;
}

h2 {
  margin-top: 14px;
  font-size: 30px;
}

.form-panel > .sz-muted {
  margin: 8px 0 28px;
}

.error {
  margin-bottom: 16px;
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
  color: var(--sz-muted);
}

.form-extra a {
  color: var(--sz-deep-green);
  font-weight: 700;
}

.demo-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
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

@media (max-width: 820px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 380px;
  }

  .form-panel {
    padding: 28px 18px;
  }
}
</style>
