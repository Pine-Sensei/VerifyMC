import { createApp, provide, ref, h } from 'vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { apiService, type ConfigResponse } from './services/api'
import './index.css'

// 全局错误处理
window.addEventListener('error', (event) => {
  console.error('Global error:', event.error)
})

window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
})

const app = createApp({
  setup() {
    const config = ref<ConfigResponse>({} as ConfigResponse);
    
    const loadConfig = async () => {
      try {
        config.value = await apiService.getConfig();
      } catch (error) {
        console.error('Failed to load config:', error);
      }
    };

    const getWsPort = (): number => {
      if (typeof config.value.wsPort === 'number' && Number.isFinite(config.value.wsPort)) {
        return config.value.wsPort
      }
      return window.location.port ? (parseInt(window.location.port, 10) + 1) : 8081
    }
    
    provide('config', config);
    provide('getWsPort', getWsPort);
    
    // 初始加载配置
    loadConfig();
  },
  render: () => h(App)
});

app.use(router)
app.use(i18n)

app.mount('#app')
