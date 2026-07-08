import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  NAlert,
  NButton,
  NCheckbox,
  NConfigProvider,
  NDrawer,
  NDrawerContent,
  NDynamicTags,
  NEmpty,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NMessageProvider,
  NModal,
  NProgress,
  NSelect,
  NSkeleton,
  NSpin,
} from 'naive-ui'

import App from './App.vue'
import router from './router'
import './styles/theme.css'
import './styles/base.css'

const app = createApp(App)

const naiveComponents = [
  ['NAlert', NAlert],
  ['NButton', NButton],
  ['NCheckbox', NCheckbox],
  ['NConfigProvider', NConfigProvider],
  ['NDrawer', NDrawer],
  ['NDrawerContent', NDrawerContent],
  ['NDynamicTags', NDynamicTags],
  ['NEmpty', NEmpty],
  ['NForm', NForm],
  ['NFormItem', NFormItem],
  ['NIcon', NIcon],
  ['NInput', NInput],
  ['NInputNumber', NInputNumber],
  ['NMessageProvider', NMessageProvider],
  ['NModal', NModal],
  ['NProgress', NProgress],
  ['NSelect', NSelect],
  ['NSkeleton', NSkeleton],
  ['NSpin', NSpin],
] as const

for (const [name, component] of naiveComponents) {
  app.component(name, component)
}

app.use(createPinia()).use(router).mount('#app')
